package io.openems.common.websocket;

import java.util.Iterator;

/**
 * Generic abstraction for WebSocket handshake data, decoupled from any specific
 * library implementation.
 */
public interface HandshakeData {

	/**
	 * Iterates over the HTTP header field names.
	 *
	 * @return an {@link Iterator} of field names
	 */
	public Iterator<String> iterateHttpFields();

	/**
	 * Gets the value of the specified HTTP header field.
	 *
	 * @param name the field name
	 * @return the field value
	 */
	public String getFieldValue(String name);

}
