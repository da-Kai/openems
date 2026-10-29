package io.openems.edge.controller.api.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NameNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

class BackendSrvResolverTest {

	@Test
	void explicitPortAndIpAddressDoNotUseSrv() {
		var lookups = new AtomicInteger();
		var resolver = new BackendSrvResolver(name -> {
			lookups.incrementAndGet();
			return List.of();
		}, new Random(0));
		var explicit = URI.create("wss://api.example.com:443/path");
		var ipv4 = URI.create("ws://127.0.0.1/path");
		var ipv6 = URI.create("ws://[::1]/path");

		assertEquals(List.of(explicit), resolver.resolve(explicit));
		assertEquals(List.of(ipv4), resolver.resolve(ipv4));
		assertEquals(List.of(ipv6), resolver.resolve(ipv6));
		assertEquals(0, lookups.get());
	}

	@Test
	void noRecordOrDnsFailureUsesConfiguredUri() {
		var uri = URI.create("wss://api.example.com/path");
		var noRecord = new BackendSrvResolver(name -> {
			assertEquals("_openems._tcp.api.example.com", name);
			return List.of();
		}, new Random(0));
		var missing = new BackendSrvResolver(name -> {
			throw new NameNotFoundException("No SRV record");
		}, new Random(0));
		var timeout = new BackendSrvResolver(name -> {
			throw new CommunicationException("DNS timeout");
		}, new Random(0));

		assertEquals(List.of(uri), noRecord.resolve(uri));
		assertEquals(List.of(uri), missing.resolve(uri));
		assertEquals(List.of(uri), timeout.resolve(uri));
	}

	@Test
	void ordersTrustedRecordsByPriorityAndWeightAndPreservesUri() {
		var random = mock(RandomGenerator.class);
		when(random.nextInt(anyInt())).thenAnswer(invocation -> invocation.<Integer>getArgument(0) - 1);
		when(random.nextLong(anyLong())).thenAnswer(invocation -> invocation.<Long>getArgument(0) - 1);
		var resolver = new BackendSrvResolver(name -> List.of(//
				"20 100 9003 later.example.com.", //
				"10 1 9001 alpha.example.com.", //
				"10 9 9002 BETA.EXAMPLE.COM.", //
				"10 0 9004 zero.example.com."), random);

		assertEquals(List.of(//
				URI.create("wss://beta.example.com:9002/a%2Fb?x=a%2Fb#fragment"), //
				URI.create("wss://alpha.example.com:9001/a%2Fb?x=a%2Fb#fragment"), //
				URI.create("wss://zero.example.com:9004/a%2Fb?x=a%2Fb#fragment"), //
				URI.create("wss://later.example.com:9003/a%2Fb?x=a%2Fb#fragment")), //
				resolver.resolve(URI.create("wss://api.example.com/a%2Fb?x=a%2Fb#fragment")));
	}

	@Test
	void zeroWeightRecordCanBeSelectedWhenRandomValueIsZero() {
		var random = mock(RandomGenerator.class);
		when(random.nextInt(anyInt())).thenReturn(0);
		when(random.nextLong(anyLong())).thenReturn(0L);
		var resolver = new BackendSrvResolver(name -> List.of(//
				"0 10 443 weighted.example.com.", //
				"0 0 443 zero.example.com."), random);

		assertEquals(List.of(URI.create("wss://zero.example.com:443"), //
				URI.create("wss://weighted.example.com:443")), //
				resolver.resolve(URI.create("wss://api.example.com")));
	}

	@Test
	void rejectsExternalApexAndMalformedTargetsWithoutFallingBack() {
		var resolver = new BackendSrvResolver(name -> List.of(//
				"0 1 443 example.com.", //
				"0 1 443 otherexample.com.", //
				"0 1 443 foo.example.com.attacker.com.", //
				"0 1 443 attacker.com.", //
				"0 1 0 api.example.com.", //
				"0 1 443 .", //
				"65536 1 443 api.example.com.", //
				"invalid"), new Random(0));

		assertEquals(List.of(), resolver.resolve(URI.create("wss://api.example.com")));
	}

	@Test
	void respectsMultiLabelAndPrivatePublicSuffixes() {
		var resolver = new BackendSrvResolver(name -> List.of(//
				"0 1 443 other.customer.co.uk.", //
				"0 1 443 attacker.co.uk."), new Random(0));
		assertEquals(List.of(URI.create("wss://other.customer.co.uk:443")), //
				resolver.resolve(URI.create("wss://api.customer.co.uk")));

		var privateSuffix = new BackendSrvResolver(name -> List.of(//
				"0 1 443 node.ours.github.io.", //
				"0 1 443 node.theirs.github.io."), new Random(0));
		assertEquals(List.of(URI.create("wss://node.ours.github.io:443")), //
				privateSuffix.resolve(URI.create("wss://api.ours.github.io")));
	}

	@Test
	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	void resolvesDnsWithAnIsolatedContextClassLoader() throws Exception {
		try (var dns = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"))) {
			dns.setSoTimeout(5000);
			var answer = CompletableFuture.runAsync(() -> {
				try {
					var request = new DatagramPacket(new byte[512], 512);
					dns.receive(request);
					var query = request.getData();
					var end = 12;
					while (query[end] != 0) {
						end += 1 + Byte.toUnsignedInt(query[end]);
					}
					end += 5;
					var response = ByteBuffer.allocate(512);
					response.put(query, 0, 2).putShort((short) 0x8180);
					response.putShort((short) 1).putShort((short) 1).putShort((short) 0).putShort((short) 0);
					response.put(query, 12, end - 12);
					response.putShort((short) 0xc00c).putShort((short) 33).putShort((short) 1).putInt(60);
					response.putShort((short) 24).putShort((short) 0).putShort((short) 1).putShort((short) 8443);
					response.put(new byte[] { 4, 'n', 'o', 'd', 'e', 7, 'e', 'x', 'a', 'm', 'p', 'l', 'e', 3, 'c', 'o',
							'm', 0 });
					dns.send(new DatagramPacket(response.array(), response.position(), request.getSocketAddress()));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			var previousProvider = System.getProperty(Context.PROVIDER_URL);
			var thread = Thread.currentThread();
			var previousClassLoader = thread.getContextClassLoader();
			var isolatedClassLoader = new ClassLoader(null) {
			};
			try {
				System.setProperty(Context.PROVIDER_URL, "dns://127.0.0.1:" + dns.getLocalPort() + "/");
				thread.setContextClassLoader(isolatedClassLoader);
				var actual = new BackendSrvResolver().resolve(URI.create("wss://api.example.com/path"));
				assertSame(isolatedClassLoader, thread.getContextClassLoader());
				assertEquals(List.of(URI.create("wss://node.example.com:8443/path")), actual);
				answer.get(5, TimeUnit.SECONDS);
			} finally {
				thread.setContextClassLoader(previousClassLoader);
				if (previousProvider == null) {
					System.clearProperty(Context.PROVIDER_URL);
				} else {
					System.setProperty(Context.PROVIDER_URL, previousProvider);
				}
			}
		}
	}

	@Test
	@ResourceLock(Resources.SYSTEM_PROPERTIES)
	void fallsBackWhenDnsServerDoesNotRespond() throws Exception {
		try (var dns = new DatagramSocket(0, InetAddress.getByName("127.0.0.1"))) {
			dns.setSoTimeout(1000);
			var previousProvider = System.getProperty(Context.PROVIDER_URL);
			try {
				System.setProperty(Context.PROVIDER_URL, "dns://127.0.0.1:" + dns.getLocalPort() + "/");
				var uri = URI.create("wss://api.example.com/path");
				var start = System.nanoTime();
				assertEquals(List.of(uri), new BackendSrvResolver().resolve(uri));
				var elapsed = System.nanoTime() - start;
				dns.receive(new DatagramPacket(new byte[512], 512));
				assertTrue(elapsed >= TimeUnit.MILLISECONDS.toNanos(1500));
				assertTrue(elapsed < TimeUnit.SECONDS.toNanos(10));
			} finally {
				if (previousProvider == null) {
					System.clearProperty(Context.PROVIDER_URL);
				} else {
					System.setProperty(Context.PROVIDER_URL, previousProvider);
				}
			}
		}
	}
}
