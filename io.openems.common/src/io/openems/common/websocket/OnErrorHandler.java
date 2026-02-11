package io.openems.common.websocket;

import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;

import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;

/**
 * Handler for WebSocket OnError event.
 */
public class OnErrorHandler implements Runnable {

	private final WebSocket ws;
	private final Exception ex;
	private final OnError onError;
	private final BiConsumer<Throwable, String> handleInternalError;

	public OnErrorHandler(//
			WebSocket ws, Exception ex, OnError onError, //
			BiConsumer<Throwable, String> handleInternalError) {
		this.ws = ws;
		this.ex = ex;
		this.onError = onError;
		this.handleInternalError = handleInternalError;
	}

	@Override
	public final void run() {
		try {
			this.onError.accept(this.ws, this.ex);

		} catch (RuntimeException e) {
			// Catch runtime exceptions thrown during error handling
			this.handleInternalError.accept(e, generateWsDataString(this.ws));
		} catch (Exception e) {
			// Catch checked exceptions (e.g., OpenemsException) from the callback
			this.handleInternalError.accept(e, generateWsDataString(this.ws));
		}
	}

}
