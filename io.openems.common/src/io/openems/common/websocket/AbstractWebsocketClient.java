package io.openems.common.websocket;

import java.net.ConnectException;
import java.net.Proxy;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;

import io.openems.common.function.BooleanConsumer;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.logger.ContextLogger;
import io.openems.common.types.ResolvedURI;
import io.openems.common.types.URISet;

/**
 * A Websocket Client implementation that automatically tries to reconnect a
 * closed connection.
 *
 * @param <T> the type of websocket attachments inheriting {@link WsData}
 */
public abstract class AbstractWebsocketClient<T extends WsData> extends AbstractWebsocket<T> {

	protected final AtomicReference<WebSocketClient> ws = new AtomicReference<>();

	private final Logger log;
	private final Proxy proxy;
	private final Draft draft;
	private final Map<String, String> httpHeaders;
	private final URISet serverUris;
	private final BooleanConsumer onConnectedChange;
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private final ClientReconnectorWorker reconnectorWorker;

	protected AbstractWebsocketClient(String name, WebsocketClientParams params) {
		super(name);
		this.log = new ContextLogger(AbstractWebsocketClient.class, name);
		this.serverUris = params.serverUri();
		this.proxy = params.proxy();
		this.draft = params.draft();
		this.httpHeaders = params.httpHeaders();
		this.onConnectedChange = params.onConnectedChange();

		// Initialize reconnector
		this.reconnectorWorker = new ClientReconnectorWorker(this, this.serverUris, params.reconnectorConfig());
	}

	private WebSocketClient createWsClient(ResolvedURI uri) {
		return new WebSocketClient(uri, this.draft, this.httpHeaders) {

			@Override
			public void onOpen(ServerHandshake handshake) {
				AbstractWebsocketClient.this.execute(new OnOpenHandler(//
						this, handshake, //
						AbstractWebsocketClient.this.getOnOpen(), //
						AbstractWebsocketClient.this::logWarn, //
						AbstractWebsocketClient.this::handleInternalError));
				this.updateIsConnected();
			}

			@Override
			public void onMessage(String message) {
				AbstractWebsocketClient.this.execute(new OnMessageHandler(//
						this, message, //
						AbstractWebsocketClient.this.getOnRequest(), //
						AbstractWebsocketClient.this.getOnNotification(), //
						AbstractWebsocketClient.this::sendMessage, //
						AbstractWebsocketClient.this::handleInternalError, //
						AbstractWebsocketClient.this::logWarn));
			}

			@Override
			public void onError(Exception ex) {
				if (ex instanceof ConnectException) {
					// Ignore. This happens when connecting fails and is handled by
					// ClientReconnectorWorker
					return;
				}

				AbstractWebsocketClient.this.execute(new OnErrorHandler(//
						this, ex, //
						AbstractWebsocketClient.this.getOnError(), //
						AbstractWebsocketClient.this::handleInternalError));
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				AbstractWebsocketClient.this.execute(new OnCloseHandler(//
						this, code, reason, remote, //
						AbstractWebsocketClient.this.getOnClose(), //
						AbstractWebsocketClient.this::handleInternalError));

				if (code == CloseFrame.NEVER_CONNECTED) {
					// Ignore "Code [-1] Reason [Connection refused: connect]"
					return;
				}

				AbstractWebsocketClient.this.log.info("WebSocket [{}] closed. Code [{}] Reason [{}]", uri, code,
						reason);
				if (this.updateIsConnected()) {
					AbstractWebsocketClient.this.reconnectorWorker.triggerNextRun();
				}
			}

			private boolean updateIsConnected() {
				var isOpen = this.isOpen();
				if (AbstractWebsocketClient.this.isConnected.getAndSet(isOpen) != isOpen) {
					// Value has changed
					AbstractWebsocketClient.this.onConnectedChange.accept(isOpen);
					return true;
				}
				return false;
			}
		};
	}

	/**
	 * Creates and configures a {@link WebSocketClient} for the given resolved URI,
	 * attaches the corresponding {@link WsData}, stores it as the current
	 * websocket, and returns it.
	 *
	 * @param uri the resolved websocket server URI to connect to
	 * @return the configured websocket client instance
	 */
	/* package */ WebSocketClient setupWebsocket(ResolvedURI uri) {
		final var websocket = this.createWsClient(uri);

		// https://github.com/TooTallNate/Java-WebSocket/wiki/Lost-connection-detection
		websocket.setConnectionLostTimeout(100);

		// initialize WsData
		var wsData = AbstractWebsocketClient.this.createWsData(websocket);
		websocket.setAttachment(wsData);

		if (this.proxy != null) {
			websocket.setProxy(this.proxy);
		}

		this.ws.set(websocket);
		return websocket;
	}

	/**
	 * Clears the currently tracked websocket and closes it if one is present.
	 *
	 * <p>
	 * This is used to reset connection state before the next reconnect attempt.
	 */
	/* package */ void resetWebsocket() {
		final var websocket = this.ws.getAndSet(null);
		if (websocket != null) {
			websocket.close();
		}
		this.isConnected.set(false);
	}

	/**
	 * Starts the websocket client.
	 */
	@Override
	public void start() {
		this.logInfo(this.log, "Opening connection to websocket server [" + this.getName() + "]");
		this.reconnectorWorker.activate(this.getName());
		this.reconnectorWorker.triggerNextRun();
	}

	/**
	 * Starts the {@link WebSocketClient}; waiting till it started.
	 *
	 * @throws InterruptedException on waiting error
	 */
	public void startBlocking() throws InterruptedException {
		final var resolvedUris = this.serverUris.resolve();
		if (resolvedUris.isEmpty()) {
			this.log.error("Unable to resolve websocket server URI");
		}
		for (var uri : resolvedUris) {
			this.log.info("Opening connection to websocket server [{}]", uri);
			final var websocket = this.setupWebsocket(uri);
			if (websocket.connectBlocking()) {
				break;
			}
			this.log.error("Unable to open connection");
		}
		this.reconnectorWorker.activate(this.getName());
	}

	/**
	 * Stops the websocket client.
	 */
	@Override
	public void stop() {
		this.log.info("Closing connection to websocket server [{}]", this.getName());
		// shutdown reconnector
		this.reconnectorWorker.deactivate();
		// close websocket
		final var websocket = this.ws.getAndSet(null);
		if (websocket != null) {
			websocket.close(CloseFrame.NORMAL, "Closing connection [" + this.getName() + "]");
		}
	}

	/**
	 * Sends a {@link JsonrpcMessage} to the {@link WebSocket}. Returns true if
	 * sending was successful, otherwise false. Also logs a warning in that case.
	 *
	 * @param message the {@link JsonrpcMessage}
	 * @return true if sending was successful
	 */
	public boolean sendMessage(JsonrpcMessage message) {
		return this.sendMessage(this.ws.get(), message);
	}

	@Override
	protected OnInternalError getOnInternalError() {
		return (t, wsData) -> this.log.error("OnInternalError for {}. {}: {}", //
				wsData, t.getClass(), t.getMessage(), t);
	}

	/**
	 * Sends a JSON-RPC Request and returns a future Response.
	 *
	 * @param request the JSON-RPC Request
	 * @return the future JSON-RPC Response
	 */
	public CompletableFuture<JsonrpcResponseSuccess> sendRequest(JsonrpcRequest request) {
		final var websocket = this.ws.get();
		if (websocket == null) {
			return CompletableFuture.failedFuture(new ConnectException("Websocket is not connected"));
		}
		WsData wsData = websocket.getAttachment();
		return wsData.send(request);
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log.
	 *
	 * @return the debug log output or null
	 */
	public String debugLog() {
		var b = new StringBuilder(64) //
				.append("[").append(this.getName()).append("] [monitor] ");
		final var websocket = this.ws.get();
		if (websocket != null && websocket.isOpen()) {
			b.append("Connected ");
		} else {
			b.append("NOT CONNECTED. ").append(this.reconnectorWorker.debugLog());
		}
		return b.toString();
	}
}
