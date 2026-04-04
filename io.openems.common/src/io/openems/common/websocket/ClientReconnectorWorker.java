package io.openems.common.websocket;

import io.openems.common.logger.LazyContextLogger;
import io.openems.common.types.URISet;
import io.openems.common.worker.AbstractWorker;
import org.java_websocket.enums.ReadyState;
import org.slf4j.Logger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ClientReconnectorWorker extends AbstractWorker {

	public record Config(int connectTimeoutSeconds, int maxWaitSeconds, int minWaitSeconds) {
	}

	public static final ClientReconnectorWorker.Config DEFAULT_CONFIG = new Config(100, 100, 10);

	private final Logger log;
	private final AbstractWebsocketClient<?> parent;
	private final Config config;
	private final URISet serverUris;

	private String debugLog = null;

	private boolean isConnected = false;

	public ClientReconnectorWorker(AbstractWebsocketClient<?> parent, URISet serverUris, Config config) {
		this.parent = parent;
		this.config = config;
		this.serverUris = serverUris;

		this.log = new LazyContextLogger(ClientReconnectorWorker.class, parent::getName);
	}

	@Override
	protected void forever() throws Exception {
		final var parentWs = this.parent.ws.get();
		this.isConnected = parentWs != null && parentWs.getReadyState() == ReadyState.OPEN;
		if (this.isConnected) {
			return;
		}

		final var start = System.currentTimeMillis();
		final var retryUris = this.serverUris.resolve();

		this.log.info("Reconnecting Websocket...");

		for (var uri : retryUris) {
			try {
				TimeUnit.SECONDS.sleep(1);
				final var ws = this.parent.initConnection(uri);

				this.log.info("# Connecting WebSocket to '{}'... Blocking[{}s]", uri, this.config.connectTimeoutSeconds());
				this.isConnected = ws.connectBlocking(this.config.connectTimeoutSeconds(), TimeUnit.SECONDS);
			} catch (IllegalStateException e) {
				this.log.warn("# Exception while connecting: {}", e.toString());
			}

			if (this.isConnected) {
				this.log.warn("# Connecting WebSocket to '{}' successfully", uri);
				break;
			}
			this.log.warn("# Connecting WebSocket to '{}' failed", uri);
			this.parent.killConnection();
		}

		if (this.isConnected) {
			final var connectionTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start);
			this.debugLog = null;
			this.log.info("Connected successfully [{}s]", connectionTime);
		} else {
			this.debugLog = "Connection failed";
			this.log.error("Connection failed");
		}
	}

	@Override
	protected int getCycleTime() {
		final var waitSeconds = ThreadLocalRandom.current().nextInt(this.config.minWaitSeconds, this.config.maxWaitSeconds + 1);
		if (!this.isConnected) {
			this.log.info("Schedule a reconnect in {}s", waitSeconds);
		}
		return waitSeconds * 1000;
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output or null
	 */
	public String debugLog() {
		var message = this.debugLog;
		return message == null //
				? "" //
				: message;
	}

}
