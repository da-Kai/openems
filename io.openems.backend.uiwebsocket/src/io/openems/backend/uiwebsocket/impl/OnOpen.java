package io.openems.backend.uiwebsocket.impl;

import io.openems.common.exceptions.OpenemsError;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final WsSessionRegistry wsSessionRegistry;

	public OnOpen(WsSessionRegistry wsSessionRegistry) {
		this.wsSessionRegistry = wsSessionRegistry;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		final var wsData = (WsData) ws.getAttachment();
		this.wsSessionRegistry.registerWsData(wsData);
		return null;
	}
}