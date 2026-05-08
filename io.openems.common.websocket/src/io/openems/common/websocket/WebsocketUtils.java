package io.openems.common.websocket;

import java.util.Optional;
import java.util.UUID;

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
		return getAsOptionalString(handshakedata, fieldName).orElse(null);
	}

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
	 * @return the field value as optional; empty if not found
	 */
	public static Optional<String> getAsOptionalString(HandshakeData handshakedata, String fieldName) {
		for (var iter = handshakedata.iterateHttpFields(); iter.hasNext();) {
			var field = iter.next();
			if (fieldName.equalsIgnoreCase(field)) {
				return Optional.of(handshakedata.getFieldValue(field).trim());
			}
		}
		return Optional.empty();
	}

	/**
	 * Gets a String value from a {@link HandshakeData}.
	 *
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshakedata the {@link HandshakeData}
	 * @param header        the header to search for
	 * @return the field value as optional; empty if not found
	 */
	public static Optional<String> getAsOptionalString(HandshakeData handshakedata, CommonHttpHeader header) {
		return getAsOptionalString(handshakedata, header.asString());
	}

	/**
	 * Gets a UUID value from a {@link HandshakeData}.
	 *
	 * <p>
	 * NOTE: Per <a href=
	 * "https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">specification</a>
	 * "Field names are case-insensitive".
	 *
	 * @param handshakedata the {@link HandshakeData}
	 * @param header        the header to search for
	 * @return the field value as optional; empty if not found or not a valid UUID
	 */
	public static Optional<UUID> getAsOptionalUuid(HandshakeData handshakedata, CommonHttpHeader header) {
		return getAsOptionalString(handshakedata, header) //
				.map((raw) -> {
					try {
						return UUID.fromString(raw);
					} catch (IllegalArgumentException e) {
						return null;
					}
				});
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
