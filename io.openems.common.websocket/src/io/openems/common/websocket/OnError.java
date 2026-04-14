package io.openems.common.websocket;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiConsumer;

@FunctionalInterface
public interface OnError extends ThrowingBiConsumer<WebsocketConnection, Exception, OpenemsException> {

	public static final OnError NO_OP = (ws, ex) -> {
	};

	/**
	 * Handles a websocket error.
	 *
	 * @param ws the {@link WebsocketConnection}
	 * @param ex the {@link Exception}
	 * @throws OpenemsException on error
	 */
	public void accept(WebsocketConnection ws, Exception ex) throws OpenemsException;

}
