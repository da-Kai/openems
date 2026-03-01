package io.openems.backend.edge.server;

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
		WsData wsData = ws.getAttachment();
		this.log.error("[{}] Websocket error. {}", wsData.getEdgeIdString(), ex.getClass().getSimpleName(), ex);
	}

}
