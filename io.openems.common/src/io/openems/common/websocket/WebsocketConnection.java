package io.openems.common.websocket;

import java.net.InetSocketAddress;

/**
 * Generic abstraction for a WebSocket connection, decoupled from any specific
 * library implementation.
 */
public interface WebsocketConnection {

	/**
	 * Sends a text message over this WebSocket connection.
	 *
	 * @param text the message text to send
	 */
	public void send(String text);

	/**
	 * Checks whether this WebSocket connection is currently open.
	 *
	 * @return true if the connection is open
	 */
	public boolean isOpen();

	/**
	 * Gets the remote socket address of the connected peer.
	 *
	 * @return the remote {@link InetSocketAddress}
	 */
	public InetSocketAddress getRemoteSocketAddress();

	/**
	 * Gets the attachment associated with this WebSocket connection.
	 *
	 * @param <T> the type of the attachment
	 * @return the attachment
	 */
	public <T> T getAttachment();

	/**
	 * Sets the attachment associated with this WebSocket connection.
	 *
	 * @param attachment the attachment to set
	 */
	public void setAttachment(Object attachment);

	/**
	 * Closes this WebSocket connection with a given code and reason.
	 *
	 * @param code   the close code
	 * @param reason the close reason message
	 */
	public void close(int code, String reason);

}
