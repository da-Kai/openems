package io.openems.edge.controller.api.backend;

import io.openems.common.types.URISet;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.WsData;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

	private final ControllerApiBackendImpl parent;
	private final OnOpen onOpen;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	protected WebsocketClient(ControllerApiBackendImpl parent, String name, URI serverUri,
	                          Map<String, String> httpHeaders, Proxy proxy) {
		this(parent, name, new URISet(serverUri), httpHeaders, proxy);
	}

	protected WebsocketClient(ControllerApiBackendImpl parent, String name, URISet serverUris,
	                          Map<String, String> httpHeaders, Proxy proxy) {
		super(name, serverUris, httpHeaders, proxy);
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = (ws, code, reason, remote) -> {
			this.log.atError().setMessage("Disconnected from OpenEMS Backend ({}) [{}{}]") //
					.addArgument(code)
					.addArgument(() -> {
						final var addr = ws.getRemoteSocketAddress();
						if (addr == null) {
							return "N/A";
						}
						return addr.getHostString();
					}) //
					.addArgument(proxy == AbstractWebsocketClient.NO_PROXY ? "" : " via Proxy") //
					.log();
			this.parent.getUnableToSendChannel().setNextValue(true);
		};
	}

	@Override
	public OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	public BackendOnRequest getOnRequest() {
		return this.parent.requestHandler;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	public OnError getOnError() {
		return this.onError;
	}

	@Override
	public OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}

	/**
	 * Checks if the WebSocket connection is currently open.
	 *
	 * @return true if connection is open
	 */
	public boolean isConnected() {
		final var websocket = this.ws.get();
		return websocket != null && websocket.isOpen();
	}

	@Override
	protected void execute(Runnable command) {
		this.parent.execute(command);
	}

	/**
	 * Schedules a command using the {@link ScheduledExecutorService}.
	 *
	 * @param command      a {@link Runnable}
	 * @param initialDelay the initial delay
	 * @param delay        the delay
	 * @param unit         the {@link TimeUnit}
	 * @return a {@link ScheduledFuture}, or null if Executor is shutting down
	 */
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
	                                                    TimeUnit unit) {
		return this.parent.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
}
