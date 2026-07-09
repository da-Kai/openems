package io.openems.common.uri;

import jdk.dynalink.linker.support.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Best-effort resolver for a fixed set of endpoint URIs.
 *
 * <p>
 * The class stores a snapshot of input URIs and resolves each URI host to one
 * or more concrete IP-based targets when {@link #resolve(URI)} is called.
 * <ul>
 * <li>If the host is already an IP literal, exactly one {@link ResolvedURI} is
 * created from it.</li>
 * <li>If the host is a DNS name, all currently resolvable A/AAAA addresses are
 * converted to {@link ResolvedURI} entries.</li>
 * </ul>
 * For DNS hosts, resolved addresses are shuffled per host to avoid relying on a
 * stable address order.
 *
 * <p>
 * This class does not fail fast: resolution problems are logged and only the
 * affected URI/address is skipped. The returned list can therefore be empty
 * even if input URIs were provided.
 */
public class URIResolver {

	private static final Logger log = LoggerFactory.getLogger(URIResolver.class);

	public enum ResolveStrategy {
		ANY, IPv4_ONLY, IPv6_ONLY, IPv4_PREFERRED, IPv6_PREFERRED
	}

	private URIResolver() {
		// utility class
	}

	private static boolean isSubdomain(String base, String subdomain) {
		if (base == null || subdomain == null) {
			return false;
		}
		if (base.equals(subdomain)) {
			return true;
		}
		if (subdomain.endsWith(".")) {
			subdomain = subdomain.substring(0, subdomain.length() - 1);
		}
		return subdomain.endsWith("." + base);
	}

	private static List<URI> resolveSRV(URI uri) {
		String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			return List.of(uri);
		}

		String srvQuery = "_openems._tcp." + host;

		Hashtable<String, String> env = new Hashtable<>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url", "dns:");

		try {
			InitialDirContext ctx = new InitialDirContext(env);
			Attributes attrs = ctx.getAttributes(srvQuery, new String[] { "SRV" });
			Attribute srvAttr = attrs.get("SRV");

			if (srvAttr == null || srvAttr.size() == 0) {
				log.debug("No _openems._tcp SRV record found. Using original URI.");
				return List.of(uri);
			}

			final var enumeration = srvAttr.getAll();
			final var resolvedUris = Stream.generate(() -> {
				try {
					return enumeration.hasMore() ? enumeration.next() : null;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}) //
					.takeWhile(java.util.Objects::nonNull) //
					.map(raw -> {
						String record = raw.toString();
						String[] parts = record.split("\\s+");
						if (parts.length >= 4) {
							int resolvedPriority = Integer.parseInt(parts[0]);
							int resolvedWeight = Integer.parseInt(parts[1]);
							int resolvedPort = Integer.parseInt(parts[2]);
							String resolvedHost = parts[3];

							if (resolvedHost.endsWith(".")) {
								resolvedHost = resolvedHost.substring(0, resolvedHost.length() - 1);
							}

							return new SRVEntry(resolvedPriority, resolvedWeight, resolvedPort, resolvedHost);
						}
						return null;
					}) //
					.filter(Objects::nonNull) //
					.sorted(SRVEntry::compareTo) //
					.map(resolvedEntry -> {
						if (isSubdomain(host, resolvedEntry.host())) {
							return null;
						}
						try {
							return resolvedEntry.toURI(uri);
						} catch (URISyntaxException e) {
							log.error("Failed to construct URI from SRV record: {}", e.getMessage());
							return null;
						}
					}).filter(Objects::nonNull).toList();

			if (resolvedUris.isEmpty()) {
				return List.of(uri);
			}

			return resolvedUris;
		} catch (Exception e) {
			log.error("SRV resolution failed: {}", e.getMessage());
			return List.of(uri);
		}
	}

	private static List<ResolvedURI> resolveDomain(URI uri, ResolveStrategy strategy) {
		final var host = uri.getHost();
		if (host == null || host.isBlank()) {
			log.warn("Unable to resolve URI {}: no host found", uri);
			return Collections.emptyList();
		}

		final InetAddress[] ips;
		try {
			ips = InetAddress.getAllByName(host);
		} catch (UnknownHostException e) {
			log.error("Unable to get A-Records from URI {}: {}", uri, e.toString());
			return Collections.emptyList();
		}

		final var primaryUris = new ArrayList<ResolvedURI>(ips.length);
		final var secondaryUris = new ArrayList<ResolvedURI>(ips.length);
		for (var ip : ips) {
			final var isIpv4 = ip instanceof Inet4Address;
			final var isIpv6 = ip instanceof Inet6Address;

			if (strategy == ResolveStrategy.IPv4_ONLY && !isIpv4) {
				return Collections.emptyList();
			}
			if (strategy == ResolveStrategy.IPv6_ONLY && !isIpv6) {
				return Collections.emptyList();
			}

			final ResolvedURI resolved;
			try {
				resolved = new ResolvedURI(uri, ip, host);
			} catch (URISyntaxException ex) {
				log.error("Unable to resolve URI with ip '{}'", ip.getHostAddress());
				return Collections.emptyList();
			}

			if (strategy == ResolveStrategy.ANY //
					|| (strategy == ResolveStrategy.IPv4_PREFERRED && isIpv4) //
					|| (strategy == ResolveStrategy.IPv6_PREFERRED && isIpv6)) {
				primaryUris.add(resolved);
			} else {
				secondaryUris.add(resolved);
			}
		}

		Collections.shuffle(primaryUris);
		final var resolvedUris = new ArrayList<>(primaryUris);
		Collections.shuffle(secondaryUris);
		resolvedUris.addAll(secondaryUris);

		return resolvedUris;
	}

	/**
	 * Resolves all configured URIs into concrete {@link ResolvedURI} entries,
	 * without filtering or ordering.
	 *
	 * <p>
	 * This method does not fail fast: URIs with missing hosts, unresolvable DNS
	 * names, or addresses that cannot be embedded into a valid URI are logged and
	 * skipped. The returned list can therefore be empty even if input URIs were
	 * provided.
	 *
	 * @param uri the URI to resolve; must not be {@code null}
	 *
	 * @return resolved URIs; never {@code null}, possibly empty
	 */
	public static List<ResolvedURI> resolve(URI uri) {
		return resolve(uri, ResolveStrategy.ANY);
	}

	/**
	 * Resolves all configured URIs into concrete {@link ResolvedURI} entries,
	 * filtering and ordering them according to the given {@code strategy}. Ordering
	 * is first by SRV weight, then by resolve strategy.
	 *
	 * <p>
	 * A single input URI can produce multiple output entries (e.g. when a DNS name
	 * has several A/AAAA records or the domain has a _openems._tcp SRV record). The
	 * result order follows the input URI iteration order. Within each URI, the
	 * strategy controls which addresses appear:
	 *
	 * <ul>
	 * <li>{@link ResolveStrategy#ANY} – all resolved addresses.</li>
	 * <li>{@link ResolveStrategy#IPv4_ONLY} – only IPv4 addresses.</li>
	 * <li>{@link ResolveStrategy#IPv6_ONLY} – only IPv6 addresses.</li>
	 * <li>{@link ResolveStrategy#IPv4_PREFERRED} – IPv4 addresses first, then IPv6
	 * addresses.</li>
	 * <li>{@link ResolveStrategy#IPv6_PREFERRED} – IPv6 addresses first, then IPv4
	 * addresses.</li>
	 * </ul>
	 *
	 * <p>
	 * This method does not fail fast: URIs with missing hosts, unresolvable DNS
	 * names, or addresses that cannot be embedded into a valid URI are logged and
	 * skipped. The returned list can therefore be empty even if input URIs were
	 * provided.
	 *
	 * @param uri      the URI to resolve; must not be {@code null}
	 * @param strategy the address-family filter / ordering strategy; must not be
	 *                 {@code null}
	 * @return resolved URIs; never {@code null}, possibly empty
	 */
	public static List<ResolvedURI> resolve(URI uri, ResolveStrategy strategy) {
		final var resolvedUris = new ArrayList<ResolvedURI>();

		final var domains = resolveSRV(uri);

		for (var domain : domains) {
			final var resolved = resolveDomain(domain, strategy);
			resolvedUris.addAll(resolved);
		}

		return resolvedUris;
	}

}
