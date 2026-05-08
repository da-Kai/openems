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
 getAsOptionalString(handshakedata, fieldName).orElse(null);
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
handshakedata.iterateHttpFields(); iter.hasNext();) {
ext();
ame.equalsIgnoreCase(field)) {
 Optional.of(handshakedata.getFieldValue(field).trim());
 Optional.empty();
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
 getAsOptionalString(handshakedata, header.asString());
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
 getAsOptionalString(handshakedata, header) //
{
 UUID.fromString(raw);
tException e) {
 null;
al String[] REMOTE_IDENTIFICATION_HEADERS = new String[] { //
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
: REMOTE_IDENTIFICATION_HEADERS) {
g(handshakedata, key);
ull) {
 value;
 ws.getRemoteSocketAddress().toString();
}

/**
 * Gets the toLogString() content of the WsData attachment of the
 * WebsocketConnection; or empty string if not available.
 *
 * @param ws the {@link WebsocketConnection}
 * @return the {@link WsData#toLogString()} content
 */
public static String generateWsDataString(WebsocketConnection ws) {
ull) {
 "";
t();
ull) {
 "";
g = wsData.toLogString();
g == null) {
 "";
 logString;
}
}
