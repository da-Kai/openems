package io.openems.edge.controller.api.backend;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.logger.ContextLogger;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log;

	public OnNotification(ControllerApiBackendImpl parent) {
		this.log = new ContextLogger(OnNotification.class, parent.id());
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsException {
		this.log.warn("Unhandled Notification: {}", notification);
	}

}
