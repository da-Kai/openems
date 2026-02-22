package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;

import io.openems.backend.common.metadata.User;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.websocket.AbstractWebsocketServer;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnOpen;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	protected final UiWebsocketImpl parent;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose = new io.openems.backend.uiwebsocket.impl.OnClose();
	private final int requestLimit;

	public WebsocketServer(UiWebsocketImpl parent, String name, int port, int poolSize, int requestLimit) {
		super(name, port, poolSize);
		this.parent = parent;
		this.onRequest = new OnRequest(parent);
		this.onNotification = new OnNotification(name, this::assertUser);
		this.onError = new OnError(name);
		this.requestLimit = requestLimit;
	}
	
	private User assertUser(WsData wsData, JsonrpcNotification notification) {
		try {
			return this.parent.assertUser(wsData, notification);
		} catch (Exception e) {
			// ignore
			return null;
		}
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws, this.requestLimit);
	}

	@Override
	protected OnOpen getOnOpen() {
		return OnOpen.NO_OP;
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
}
