package io.openems.common.websocket;

public class WebsocketUtils {

	/**
	 * Gets a String value from a {@link HandshakeData}.
	 * 
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshakedata the {@link HandshakeData}
	 * @param fieldName     the name of the field
	 * @return the field value; or null
	 */
	public static String getAsString(HandshakeData handshakedata, String fieldName) {
		for (var iter = handshakedata.iterateHttpFields(); iter.hasNext();) {
			var field = iter.next();
			if (fieldName.equalsIgnoreCase(field)) {
				return handshakedata.getFieldValue(field).trim();
			}
		}
		return null;
	}

	private static final String[] REMOTE_IDENTIFICATION_HEADERS = new String[] { //
			"Forwarded", "X-Forwarded-For", "X-Real-IP" };

	/**
	 * Parses a identifier for the Remote from the {@link HandshakeData}.
	 * 
	 * <p>
	 * Tries to use the headers "Forwarded", "X-Forwarded-For" or "X-Real-IP". Falls
	 * back to `ws.getRemoteSocketAddress()`. See https://serverfault.com/a/920060
	 * 
	 * @param ws            the {@link WebsocketConnection}
	 * @param handshakedata the {@link HandshakeData}
	 * @return an identifier String
	 */
	public static String parseRemoteIdentifier(WebsocketConnection ws, HandshakeData handshakedata) {
		for (var key : REMOTE_IDENTIFICATION_HEADERS) {
			var value = getAsString(handshakedata, key);
			if (value != null) {
				return value;
			}
		}
		// fallback
		return ws.getRemoteSocketAddress().toString();
	}

	/**
	 * Gets the toLogString() content of the WsData attachment of the
	 * WebsocketConnection; or empty string if not available.
	 *
	 * @param ws the {@link WebsocketConnection}
	 * @return the {@link WsData#toLogString()} content
	 */
	public static String generateWsDataString(WebsocketConnection ws) {
		if (ws == null) {
			return "";
		}
		WsData wsData = ws.getAttachment();
		if (wsData == null) {
			return "";
		}
		var logString = wsData.toLogString();
		if (logString == null) {
			return "";
		}
		return logString;
	}
}
