package io.openems.common.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger(URISet.class);

	private static boolean isHostIP(String host) {
		if (host == null) {
			return false;
		}
		return host.matches("^[0-9.]+$") || host.matches("^[0-9a-fA-F:]+$");
	}

	private final List<URI> uris;

	public URISet(URI... uris) {
		this(List.of(uris));
	}

	public URISet(List<URI> uris) {
		this.uris = List.copyOf(uris);
	}

	/**
	 * Resolves all configured URIs into concrete {@link ResolvedURI} entries.
	 *
	 * <p>
	 * A single input URI can produce multiple output entries (e.g. multiple
	 * A/AAAA records). The result order follows the input URI iteration order;
	 * for each DNS host, the resolved addresses are added in shuffled order.
	 * Unresolvable hosts or invalid transformed URIs are logged and omitted.
	 *
	 * @return resolved URIs; never {@code null}, possibly empty
	 */
	public List<ResolvedURI> resolve() {
		final var resolvedUris = new ArrayList<ResolvedURI>();

		for (var uri : this.uris) {
			final var host = uri.getHost();

			if (isHostIP(host)) {
				try {
					InetAddress ipAddr = InetAddress.getByName(host);
					resolvedUris.add(new ResolvedURI(uri, ipAddr));
				} catch (Exception ex) {
					log.error("Unable to cast {} to ip-address", host);
				}
				continue;
			}

			final InetAddress[] ips;
			try {
				ips = InetAddress.getAllByName(host);
			} catch (UnknownHostException e) {
				log.error("Unable to get A-Records from URI {}: {}", uri, e.toString());
				continue;
			}

			final var updatedUris = new ArrayList<ResolvedURI>();
			for (var ip : ips) {
				try {
					updatedUris.add(new ResolvedURI(uri, ip, host));
				} catch (URISyntaxException ex) {
					log.error("Unable to resolve URI with ip '{}'", ip.getHostAddress());
				}
			}
			Collections.shuffle(updatedUris);
			resolvedUris.addAll(updatedUris);
		}

		return resolvedUris;
	}

}
