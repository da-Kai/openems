package io.openems.common.websocket;

import java.util.function.Function;

import org.java_websocket.handshake.ClientHandshake;

/**
 * Validates an incoming WebSocket handshake request before the connection is
 * fully established. This allows rejecting connections during the handshake
 * phase (e.g. for invalid API keys) without accepting and then immediately
 * closing them.
 */
@FunctionalInterface
public interface OnHandshake extends Function<ClientHandshake, String> {

	public static final OnHandshake NO_OP = (handshake) -> null;

	/**
	 * Validates the given {@link ClientHandshake}.
	 *
	 * @param handshake the {@link ClientHandshake}
	 * @return null if the handshake should be accepted, or an error message to
	 *         reject the connection
	 */
	@Override
	public String apply(ClientHandshake handshake);
}
