package io.openems.common.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Best-effort resolver for a fixed set of endpoint URIs.
 *
 * <p>
 * The class stores a snapshot of input URIs and resolves each URI host to one
 * or more concrete IP-based targets when {@link #resolve()} is called.
 * <ul>
 * <li>If the host is already an IP literal, exactly one {@link ResolvedURI}
 * is created from it.</li>
 * <li>If the host is a DNS name, all currently resolvable A/AAAA addresses are
 * converted to {@link ResolvedURI} entries.</li>
 * </ul>
 * For DNS hosts, resolved addresses are shuffled per host to avoid relying on
 * a stable address order.
 *
 * <p>
 * This class does not fail fast: resolution problems are logged and only the
 * affected URI/address is skipped. The returned list can therefore be empty
 * even if input URIs were provided.
 */
public class URISet {

	public enum ResolveStrategy {
		ANY, IPv4_ONLY, IPv6_ONLY, IPv4_PREFERRED, IPv6_PREFERRED
	}

	private static final Logger log = LoggerFactory.getLogger(URISet.class);

	private final List<URI> uris;

	public URISet(URI... uris) {
		this(List.of(uris));
	}

	public URISet(List<URI> uris) {
		this.uris = List.copyOf(uris);
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
	 * @return resolved URIs; never {@code null}, possibly empty
	 */
	public List<ResolvedURI> resolve() {
		return this.resolve(ResolveStrategy.ANY);
	}

	/**
	 * Resolves all configured URIs into concrete {@link ResolvedURI} entries,
	 * filtering and ordering them according to the given {@code strategy}.
	 *
	 * <p>
	 * A single input URI can produce multiple output entries (e.g. when a DNS name
	 * has several A/AAAA records). The result order follows the input URI iteration
	 * order. Within each URI, the strategy controls which addresses appear:
	 *
	 * <ul>
	 * <li>{@link ResolveStrategy#ANY} – all resolved addresses.</li>
	 * <li>{@link ResolveStrategy#IPv4_ONLY} – only IPv4 addresses.</li>
	 * <li>{@link ResolveStrategy#IPv6_ONLY} – only IPv6 addresses.</li>
	 * <li>{@link ResolveStrategy#IPv4_PREFERRED} – IPv4 addresses first, then IPv6 addresses.</li>
	 * <li>{@link ResolveStrategy#IPv6_PREFERRED} – IPv6 addresses first, then IPv4 addresses.</li>
	 * </ul>
	 *
	 * <p>
	 * This method does not fail fast: URIs with missing hosts, unresolvable DNS
	 * names, or addresses that cannot be embedded into a valid URI are logged and
	 * skipped. The returned list can therefore be empty even if input URIs were
	 * provided.
	 *
	 * @param strategy the address-family filter / ordering strategy; must not be
	 *                 {@code null}
	 * @return resolved URIs; never {@code null}, possibly empty
	 */
	public List<ResolvedURI> resolve(ResolveStrategy strategy) {
		final var resolvedUris = new ArrayList<ResolvedURI>();

		for (var uri : this.uris) {
			final var host = uri.getHost();
			if (host == null || host.isBlank()) {
				log.warn("Unable to resolve URI {}: no host found", uri);
				continue;
			}

			final InetAddress[] ips;
			try {
				ips = InetAddress.getAllByName(host);
			} catch (UnknownHostException e) {
				log.error("Unable to get A-Records from URI {}: {}", uri, e.toString());
				continue;
			}

			final var primaryUris = new ArrayList<ResolvedURI>(ips.length);
			final var secondaryUris = new ArrayList<ResolvedURI>(ips.length);
			for (var ip : ips) {
				final var isIpv4 = ip instanceof Inet4Address;
				final var isIpv6 = ip instanceof Inet6Address;

				if (strategy == ResolveStrategy.IPv4_ONLY && !isIpv4) {
					continue;
				}
				if (strategy == ResolveStrategy.IPv6_ONLY && !isIpv6) {
					continue;
				}

				final ResolvedURI resolved;
				try {
					resolved = new ResolvedURI(uri, ip, host);
				} catch (URISyntaxException ex) {
					log.error("Unable to resolve URI with ip '{}'", ip.getHostAddress());
					continue;
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
			resolvedUris.addAll(primaryUris);
			Collections.shuffle(secondaryUris);
			resolvedUris.addAll(secondaryUris);
		}

		return resolvedUris;
	}

}
