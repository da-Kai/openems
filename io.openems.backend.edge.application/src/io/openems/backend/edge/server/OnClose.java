package io.openems.backend.edge.server;

import java.util.function.Consumer;

import org.java_websocket.WebSocket;

public class OnClose implements io.openems.common.websocket.OnClose {

	private final Consumer<WebSocket> onClose;

	public OnClose(Consumer<WebSocket> onClose) {
		this.onClose = onClose;
	}

	@Override
	public void accept(WebSocket ws, int code, String reason, boolean remote) {
		this.onClose.accept(ws);
	}

}
