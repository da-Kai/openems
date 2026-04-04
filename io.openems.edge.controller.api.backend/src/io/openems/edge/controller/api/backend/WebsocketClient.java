package io.openems.edge.controller.api.backend;

import io.openems.common.websocket.AbstractWebsocketClient;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.WebsocketClientParams;
import io.openems.common.websocket.WsData;
import io.openems.edge.common.component.OpenemsComponent;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

	private final Logger log;

	private final ControllerApiBackendImpl parent;
	private final OnOpen onOpen;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	protected WebsocketClient(ControllerApiBackendImpl parent, String name, WebsocketClientParams params) {
		super(name, params);
		this.log = OpenemsComponent.getComponentLogger(WebsocketClient.class, parent);
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
					.addArgument(params.proxy() == WebsocketClientParams.NO_PROXY ? "" : " via Proxy") //
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
}
