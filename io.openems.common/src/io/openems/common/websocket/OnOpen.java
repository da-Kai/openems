package io.openems.common.websocket;

import java.util.function.BiFunction;

import io.openems.common.exceptions.OpenemsError;

@FunctionalInterface
public interface OnOpen extends BiFunction<WebsocketConnection, HandshakeData, OpenemsError> {

	public static final OnOpen NO_OP = (ws, handshakedata) -> {
		return null;
	};

	/**
	 * Handles OnOpen event of WebSocket.
	 *
	 * @param ws            the {@link WebsocketConnection}
	 * @param handshakedata the {@link HandshakeData} with HTTP headers
	 * @return {@link OpenemsError} or null
	 */
	public OpenemsError apply(WebsocketConnection ws, HandshakeData handshakedata);
}