package io.openems.backend.edge.server;

import static io.openems.common.websocket.WebsocketUtils.getAsString;
import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.websocket.HandshakeData;
import io.openems.common.websocket.WebsocketConnection;

public class OnOpen implements io.openems.common.websocket.OnOpen {

private static final int CLOSE_POLICY_VIOLATION = 1008; // RFC 6455 Policy Violation

private final Logger log = LoggerFactory.getLogger(OnOpen.class);
private final Runnable connectedEdgesChanged;

public OnOpen(Runnable connectedEdgesChanged) {
nectedEdgesChanged = connectedEdgesChanged;
}

@Override
public OpenemsError apply(WebsocketConnection ws, HandshakeData handshakedata) {
from handshake
al var apikey = getAsString(handshakedata, "apikey");

(ws, apikey);
ull) {
, new StringBuilder() //
d("Connection to backend failed. Apikey [") //
d(apikey).append("]. Remote [") //
d(parseRemoteIdentifier(ws, handshakedata)) //
d("] Error: ").append(error.name()) //
g());
 error;
}

private OpenemsError _apply(WebsocketConnection ws, String apikey) {
t
t();

ticate apikey
(edgeId == null) {
 OpenemsError.COMMON_AUTHENTICATION_FAILED;
 " + edgeId);

nectedEdgesChanged.run();

 null; // No error
}
}
