package io.openems.edge.controller.api.websocket;

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
		// get websocket attachment
		WsData wsData = ws.getAttachment();
		var user = wsData.getUser();

		// print log message
		if (user.isPresent()) {
			this.log.info("User [{}] closed websocket connection.", user.get());
		} else {
			this.log.info("Unknown User [{}] closed websocket connection.", wsData.getSessionToken());
		}
	}

}
