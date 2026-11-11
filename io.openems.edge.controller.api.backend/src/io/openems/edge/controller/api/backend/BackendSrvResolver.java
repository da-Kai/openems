package io.openems.edge.controller.api.backend;

import java.net.IDN;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

final class BackendSrvResolver {

	@FunctionalInterface
	interface Lookup {
		List<String> lookup(String name) throws NamingException;
	}

	private record SrvRecord(int priority, int weight, int port, String host) {
	}

	private static final Logger LOG = LoggerFactory.getLogger(BackendSrvResolver.class);
	private static final String SRV_PREFIX = "_openems._tcp.";
	private final Lookup lookup;
	private final RandomGenerator random;

	BackendSrvResolver() {
		this(BackendSrvResolver::lookup, new Random());
	}

	BackendSrvResolver(Lookup lookup, RandomGenerator random) {
		this.lookup = lookup;
		this.random = random;
	}

	List<URI> resolve(URI uri) {
		var host = uri.getHost();
		if (uri.getPort() != -1 || host == null || InetAddresses.isUriInetAddress(host)) {
			return List.of(uri);
		}

		List<String> answers;
		try {
			answers = this.lookup.lookup(SRV_PREFIX + host);
		} catch (NameNotFoundException e) {
			return List.of(uri);
		} catch (NamingException e) {
			LOG.warn("SRV lookup for [{}] failed; using the configured URI: {}", host, e.getMessage());
			return List.of(uri);
		}
		if (answers.isEmpty()) {
			return List.of(uri);
		}

		InternetDomainName registrableDomain;
		try {
			registrableDomain = InternetDomainName.from(host).topPrivateDomain();
		} catch (IllegalArgumentException | IllegalStateException e) {
			LOG.warn("Cannot determine a registrable domain for SRV lookup of [{}]", host);
			return List.of();
		}

		var records = new ArrayList<SrvRecord>();
		for (var answer : answers) {
			try {
				var fields = answer.trim().split("\\s+");
				if (fields.length != 4) {
					throw new IllegalArgumentException("Expected priority, weight, port and target");
				}
				var priority = parseUnsignedShort(fields[0]);
				var weight = parseUnsignedShort(fields[1]);
				var port = parseUnsignedShort(fields[2]);
				if (port == 0 || fields[3].equals(".")) {
					throw new IllegalArgumentException("Service is unavailable or has an invalid port");
				}
				var target = InternetDomainName.from(IDN.toASCII(fields[3], IDN.USE_STD3_ASCII_RULES));
				if (!target.isUnderPublicSuffix() || !target.topPrivateDomain().equals(registrableDomain)
						|| target.equals(registrableDomain)) {
					throw new IllegalArgumentException("Target is outside the configured registrable domain");
				}
				records.add(new SrvRecord(priority, weight, port, target.toString()));
			} catch (IllegalArgumentException e) {
				LOG.warn("Ignoring SRV record [{}] for [{}]: {}", answer, host, e.getMessage());
			}
		}

		var result = new ArrayList<URI>();
		records.sort(Comparator.comparingInt(SrvRecord::priority));
		while (!records.isEmpty()) {
			var priority = records.getFirst().priority();
			var group = new ArrayList<SrvRecord>();
			while (!records.isEmpty() && records.getFirst().priority() == priority) {
				group.add(records.removeFirst());
			}
			for (var i = group.size() - 1; i > 0; i--) {
				var index = this.random.nextInt(i + 1);
				var item = group.get(i);
				group.set(i, group.get(index));
				group.set(index, item);
			}
			group.sort(Comparator.comparingInt(record -> record.weight() == 0 ? 0 : 1));
			while (!group.isEmpty()) {
				var total = group.stream().mapToLong(SrvRecord::weight).sum();
				var selected = this.random.nextLong(total + 1);
				long cumulative = 0;
				for (var i = 0; i < group.size(); i++) {
					cumulative += group.get(i).weight();
					if (cumulative >= selected) {
						result.add(toUri(uri, group.remove(i)));
						break;
					}
				}
			}
		}
		if (result.isEmpty()) {
			LOG.warn("No trusted SRV targets found for [{}]", host);
		}
		return List.copyOf(result);
	}

	private static int parseUnsignedShort(String value) {
		var result = Integer.parseInt(value);
		if (result < 0 || result > 65535) {
			throw new IllegalArgumentException("SRV field is outside the unsigned 16-bit range");
		}
		return result;
	}

	private static URI toUri(URI original, SrvRecord record) {
		var result = new StringBuilder(original.getScheme()).append("://");
		if (original.getRawUserInfo() != null) {
			result.append(original.getRawUserInfo()).append('@');
		}
		result.append(record.host()).append(':').append(record.port());
		if (original.getRawPath() != null) {
			result.append(original.getRawPath());
		}
		if (original.getRawQuery() != null) {
			result.append('?').append(original.getRawQuery());
		}
		if (original.getRawFragment() != null) {
			result.append('#').append(original.getRawFragment());
		}
		return URI.create(result.toString());
	}

	private static List<String> lookup(String name) throws NamingException {
		var thread = Thread.currentThread();
		var previousClassLoader = thread.getContextClassLoader();
		try {
			// JNDI discovers the JDK DNS provider through the thread context class loader.
			thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
			var environment = new Hashtable<String, String>();
			environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
			environment.put("com.sun.jndi.dns.timeout.initial", "2000");
			environment.put("com.sun.jndi.dns.timeout.retries", "1");
			var context = new InitialDirContext(environment);
			try {
				var attribute = context.getAttributes(name, new String[] { "SRV" }).get("SRV");
				if (attribute == null) {
					return List.of();
				}
				var result = new ArrayList<String>();
				var values = attribute.getAll();
				try {
					while (values.hasMore()) {
						result.add((String) values.next());
					}
				} finally {
					values.close();
				}
				return result;
			} finally {
				context.close();
			}
		} finally {
			thread.setContextClassLoader(previousClassLoader);
		}
	}
}
