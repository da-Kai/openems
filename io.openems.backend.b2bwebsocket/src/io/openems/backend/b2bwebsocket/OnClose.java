package io.openems.backend.b2bwebsocket;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.logger.ContextLogger;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Logger log;

	public OnClose(String name) {
		this.log = new ContextLogger(OnClose.class, name);
	}

	@Override
	public void accept(WebSocket ws, int code, String reason, boolean remote) {
		WsData wsData = ws.getAttachment();
		var user = wsData.getUserOpt();
		if (user.isPresent()) {
			this.log.info("User [{}] closed connection", user.get().getName());
		} else {
			this.log.info("Connection closed");
		}
	}

}
