package io.openems.common.websocket.adapter;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;

import io.openems.common.websocket.WebsocketConnection;

/**
 * Adapter that wraps a {@link WebSocket} from the java_websocket library as a
 * {@link WebsocketConnection}.
 */
public class WebsocketConnectionAdapter implements WebsocketConnection {

	private final WebSocket ws;

	public WebsocketConnectionAdapter(WebSocket ws) {
		this.ws = ws;
	}

	@Override
	public void send(String text) {
		this.ws.send(text);
	}

	@Override
	public boolean isOpen() {
		return this.ws.isOpen();
	}

	@Override
	public InetSocketAddress getRemoteSocketAddress() {
		return this.ws.getRemoteSocketAddress();
	}

	@Override
	public <T> T getAttachment() {
		return this.ws.getAttachment();
	}

	@Override
	public void setAttachment(Object attachment) {
		this.ws.setAttachment(attachment);
	}

	@Override
	public void close(int code, String reason) {
		this.ws.close(code, reason);
	}

	/**
	 * Gets the underlying {@link WebSocket} from the java_websocket library.
	 * 
	 * <p>
	 * This should only be used within the adapter sub-package.
	 *
	 * @return the underlying {@link WebSocket}
	 */
	public WebSocket getWebSocket() {
		return this.ws;
	}

}
