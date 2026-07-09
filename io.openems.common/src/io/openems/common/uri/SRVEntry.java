package io.openems.common.uri;

import java.net.URI;
import java.net.URISyntaxException;

public record SRVEntry(int priority, int weight, int port, String host) implements Comparable<SRVEntry> {

	/**
	 * Creates a new URI based on the given base URI, replacing its host and port with
	 * the values from this SRVEntry.
	 *
	 * @param base the base URI to use
	 * @return a new URI with the host and port replaced
	 * @throws URISyntaxException if the resulting URI is invalid
	 */
	public URI toURI(URI base) throws URISyntaxException {
		return new URI(
				base.getScheme(),
				base.getUserInfo(),
				this.host,
				this.port,
				base.getPath(),
				base.getQuery(),
				base.getFragment()
		);
	}

	@Override
	public int compareTo(SRVEntry o) {
		return Integer.compare(this.priority, o.priority);
	}
}
