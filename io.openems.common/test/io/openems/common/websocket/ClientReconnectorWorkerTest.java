package io.openems.common.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ServerSocket;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class ClientReconnectorWorkerTest {

	private record Attempt(URI uri, long timeNanos) {
	}

	@Test
	void triesAllEndpointsOnInitialConnectAndAfterDisconnect() throws Exception {
		var opened = new LinkedBlockingQueue<WebSocket>();
		try (var server = DummyWebsocketServer.create().onOpen((ws, handshake) -> {
			opened.add(ws);
			return null;
		}).build()) {
			server.start();
			var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (server.getPort() == 0 && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}
			assertTrue(server.getPort() > 0);

			int closedPort;
			try (var socket = new ServerSocket(0)) {
				closedPort = socket.getLocalPort();
			}
			var unavailable = URI.create("ws://127.0.0.1:" + closedPort);
			var available = URI.create("ws://127.0.0.1:" + server.getPort());
			var client = new TestClient(List.of(unavailable, available));
			try {
				client.start();
				assertAttempts(client, unavailable, available);
				var connection = opened.poll(10, TimeUnit.SECONDS);
				assertNotNull(connection);
				connection.close();

				assertAttempts(client, unavailable, available);
				assertNotNull(opened.poll(10, TimeUnit.SECONDS));
				assertNotNull(client.ws.getAttachment());
				assertTrue(client.resolutions.get() >= 2);
			} finally {
				client.stop();
			}
		}
	}

	@Test
	void movesOnAfterHandshakeTimeout() throws Exception {
		var opened = new LinkedBlockingQueue<WebSocket>();
		try (var stalled = new ServerSocket(0); //
				var server = DummyWebsocketServer.create().onOpen((ws, handshake) -> {
					opened.add(ws);
					return null;
				}).build()) {
			server.start();
			var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
			while (server.getPort() == 0 && System.nanoTime() < deadline) {
				Thread.sleep(10);
			}
			assertTrue(server.getPort() > 0);

			var unavailable = URI.create("ws://127.0.0.1:" + stalled.getLocalPort());
			var available = URI.create("ws://127.0.0.1:" + server.getPort());
			var client = new TestClient(List.of(unavailable, available));
			try {
				client.start();
				assertAttempts(client, unavailable, available);
				assertNotNull(opened.poll(10, TimeUnit.SECONDS));
				assertNotNull(client.ws.getAttachment());
			} finally {
				client.stop();
			}
		}
	}

	private static void assertAttempts(TestClient client, URI first, URI second) throws InterruptedException {
		var firstAttempt = client.attempts.poll(10, TimeUnit.SECONDS);
		var secondAttempt = client.attempts.poll(10, TimeUnit.SECONDS);
		assertNotNull(firstAttempt);
		assertNotNull(secondAttempt);
		assertEquals(first, firstAttempt.uri());
		assertEquals(second, secondAttempt.uri());
		assertTrue(secondAttempt.timeNanos() - firstAttempt.timeNanos() >= TimeUnit.SECONDS.toNanos(2));
	}

	private static class TestClient extends AbstractWebsocketClient<WsData> {

		private final List<URI> uris;
		private final LinkedBlockingQueue<Attempt> attempts = new LinkedBlockingQueue<>();
		private final AtomicInteger resolutions = new AtomicInteger();

		private TestClient(List<URI> uris) {
			super("FailoverTest", uris.getFirst(), Map.of(), null, new ClientReconnectorWorker.Config(3, 1, 1, e -> {
			}));
			this.uris = uris;
		}

		@Override
		protected List<URI> resolveConnectionUris() {
			this.resolutions.incrementAndGet();
			return this.uris;
		}

		@Override
		void setConnectionUri(URI uri) {
			super.setConnectionUri(uri);
			this.attempts.add(new Attempt(uri, System.nanoTime()));
		}

		@Override
		protected WsData createWsData(WebSocket ws) {
			return new WsData(ws);
		}

		@Override
		protected OnOpen getOnOpen() {
			return OnOpen.NO_OP;
		}

		@Override
		protected OnRequest getOnRequest() {
			return OnRequest.NO_OP;
		}

		@Override
		protected OnNotification getOnNotification() {
			return OnNotification.NO_OP;
		}

		@Override
		protected OnError getOnError() {
			return OnError.NO_OP;
		}

		@Override
		protected OnClose getOnClose() {
			return OnClose.NO_OP;
		}

		@Override
		protected void execute(Runnable command) {
			command.run();
		}

		@Override
		protected void logInfo(Logger log, String message) {
			log.info(message);
		}

		@Override
		protected void logWarn(Logger log, String message) {
			log.warn(message);
		}

		@Override
		protected void logError(Logger log, String message) {
			log.error(message);
		}
	}
}
