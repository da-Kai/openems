package io.openems.common.uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

	/**
	 * Order a List of SRVEntries according to RFC 2782 selection algorithm.
	 *
	 * @param entries to order
	 * @return orderes list
	 */
	public static List<SRVEntry> ordered(List<SRVEntry> entries) {
		var result = new ArrayList<SRVEntry>();

		var grouped = entries.stream()
				.collect(Collectors.groupingBy(
						SRVEntry::priority,
						TreeMap::new,
						Collectors.toList()));

		var random = ThreadLocalRandom.current();

		for (var samePriority : grouped.values()) {
			var remaining = new ArrayList<>(samePriority);

			int totalWeight = remaining.stream()
					.mapToInt(SRVEntry::weight)
					.sum();

			while (!remaining.isEmpty()) {
				if (totalWeight == 0) {
					Collections.shuffle(remaining);
					result.addAll(remaining);
					break;
				}

				int value = random.nextInt(totalWeight+1);
				int cumulative = 0;

				Iterator<SRVEntry> it = remaining.iterator();

				while (it.hasNext()) {
					SRVEntry entry = it.next();

					cumulative += entry.weight();

					if (value <= cumulative) {
						result.add(entry);
						totalWeight -= entry.weight();
						it.remove();
						break;
					}
				}
			}
		}

		return result;
	}
}
