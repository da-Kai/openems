package io.openems.common.websocket;

import static io.openems.common.websocket.WebsocketUtils.generateWsDataString;

import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;

import io.openems.common.logger.ContextLogger;

/**
 * Handler for WebSocket OnOpen event.
 */
public final class OnOpenHandler implements Runnable {

	private final Logger log;
	private final WebSocket ws;
	private final Handshakedata handshake;
	private final OnOpen onOpen;
	private final BiConsumer<Throwable, String> handleInternalError;

	public OnOpenHandler(//
			String name, WebSocket ws, Handshakedata handshake, OnOpen onOpen, //
			BiConsumer<Throwable, String> handleInternalError) {
		this.ws = ws;
		this.handshake = handshake;
		this.onOpen = onOpen;
		this.log = new ContextLogger(OnOpenHandler.class, name);
		this.handleInternalError = handleInternalError;
	}

	@Override
	public final void run() {
		try {
			var error = this.onOpen.apply(this.ws, this.handshake);
			if (error != null) {
				this.log.warn("Error during OnOpen of {}", generateWsDataString(this.ws));
			}

		} catch (WebsocketNotConnectedException e) {
			this.log.warn("Websocket was closed before it has been fully opened: {}", generateWsDataString(this.ws));

		} catch (Throwable t) {
			this.handleInternalError.accept(t, generateWsDataString(this.ws));
		}
	}
}