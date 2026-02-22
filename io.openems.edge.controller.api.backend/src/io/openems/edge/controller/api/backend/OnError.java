package io.openems.edge.controller.api.backend;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.logger.ContextLogger;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log;

	public OnError(ControllerApiBackendImpl parent) {
		this.log = new ContextLogger(OnError.class, parent.id());
	}

	@Override
	public void accept(WebSocket ws, Exception ex) throws OpenemsException {
		this.log.warn("Error: {}", ex.getMessage());
	}

}
