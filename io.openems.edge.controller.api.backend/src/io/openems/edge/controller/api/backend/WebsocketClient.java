package io.openems.edge.controller.api.backend;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.logger.ContextLogger;
import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.WsData;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log;

	private final ControllerApiBackendImpl parent;
	private final OnOpen onOpen;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	protected WebsocketClient(ControllerApiBackendImpl parent, String name, URI serverUri,
			Map<String, String> httpHeaders, Proxy proxy) {
		super(name, serverUri, httpHeaders, proxy);
		this.log = new ContextLogger(WebsocketClient.class, name);
		this.parent = parent;
		this.onOpen = new OnOpen(parent);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = (ws, code, reason, remote) -> {
			this.log.error("Disconnected from OpenEMS Backend [{}{}]", // 
					serverUri, //
					proxy == AbstractWebsocketClient.NO_PROXY ? "" : " via Proxy");
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
	protected WsData createWsData(WebSocket es) {
		return new WsData(ws);
	}

	public boolean isConnected() {
		return this.ws.isOpen();
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

	@Override
	protected Logger getLogger() {
		return this.log;
	}
}
