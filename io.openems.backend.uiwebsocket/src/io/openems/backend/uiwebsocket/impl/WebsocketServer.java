package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnOpen;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	protected final UiWebsocketImpl parent;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;
	private final OnOpen onOpen;
	private final int requestLimit;

	public WebsocketServer(UiWebsocketImpl parent, WsSessionRegistry sessionRegistry, String name, int port, int poolSize, int requestLimit) {
		super(name, port, poolSize);
		this.parent = parent;
		this.onRequest = new OnRequest(parent, sessionRegistry);
		this.onNotification = new OnNotification(parent);
		this.onError = new OnError(parent);
		this.onClose = new io.openems.backend.uiwebsocket.impl.OnClose(sessionRegistry);
		this.onOpen = new io.openems.backend.uiwebsocket.impl.OnOpen(sessionRegistry);
		this.requestLimit = requestLimit;
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws, this.requestLimit);
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}
}
