package io.openems.edge.controller.api.websocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.logger.ContextLogger;

public class OnError implements io.openems.common.websocket.OnError {

	private final Logger log;

	public OnError(String name) {
		this.log = new ContextLogger(OnError.class, name);
	}

	@Override
	public void accept(WebSocket ws, Exception ex) throws OpenemsException {
		// get websocket attachment
		WsData wsData = ws.getAttachment();
		var user = wsData.getUser();

		if (user.isPresent()) {
			this.log.warn("User [{}] error: {}", user.get().getName(), ex.getMessage());
		} else {
			this.log.warn("Unknown User [{}] error: {}", wsData.getSessionToken(), ex.getMessage());
		}
	}

}
