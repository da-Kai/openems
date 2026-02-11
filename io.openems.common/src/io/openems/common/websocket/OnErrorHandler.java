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

		} catch (Throwable t) {
			// Catch both Exception (runtime and checked) and Error (e.g., OutOfMemoryError)
			// to ensure the error handler itself doesn't fail catastrophically.
			// While catching Throwable is generally discouraged, it's appropriate here
			// because this is error handling code that needs maximum robustness.
			this.handleInternalError.accept(t, generateWsDataString(this.ws));
		}
	}

}
