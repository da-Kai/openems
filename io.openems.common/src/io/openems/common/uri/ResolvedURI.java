package io.openems.common.uri;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Immutable value object for a resolved endpoint URI.
 *
 * <p>
 * The returned {@link #uri()} always has its host replaced by the resolved IP
 * address (see {@link #ip()}). If resolution started from a DNS hostname, the
 * original hostname can be obtained via {@link #host()}.
 *
 * <p>
 * Important for clients: when connecting via TLS/SSL or using Layer-7 routing,
 * the original hostname must be manually re-applied where required (e.g. TLS
 * SNI / hostname verification and HTTP {@code Host} header), because
 * {@link #uri()} itself contains the IP as host.
 */
public final class ResolvedURI {

	private static URI uriWithIp(URI uri, InetAddress ip) throws URISyntaxException {
		return new URI(uri.getScheme(), uri.getUserInfo(), ip.getHostAddress(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
	}

	private final URI uri;
	private final String host;
	private final InetAddress ip;

	/*package*/ ResolvedURI(URI uri, InetAddress ip, String host) throws URISyntaxException {
		this.ip = ip;
		this.host = host == null || ip.getHostAddress().equals(host) || host.contains(":") ? null : host;
		this.uri = uriWithIp(uri, ip);
	}

	/**
	 * Returns the original hostname before host replacement.
	 *
	 * @return the original DNS hostname, or empty if the input already used an IP
	 *         literal
	 */
	public Optional<String> host() {
		return Optional.ofNullable(this.host);
	}

	/**
	 * Returns the resolved IP address used in {@link #uri()}.
	 *
	 * @return resolved IP address
	 */
	public InetAddress ip() {
		return this.ip;
	}

	/**
	 * Returns a URI that is equivalent to the input URI, except that its host is
	 * replaced by {@link #ip()}.
	 *
	 * @return URI with IP host
	 */
	public URI uri() {
		return this.uri;
	}

	@Override
	public String toString() {
		if (this.host != null) {
			return this.uri.toString() + "[" + this.host + "]";
		}
		return this.uri.toString();
	}
}
