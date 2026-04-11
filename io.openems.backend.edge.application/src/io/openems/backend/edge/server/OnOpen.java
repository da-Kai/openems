package io.openems.backend.edge.server;

import static io.openems.common.websocket.WebsocketUtils.getAsString;

import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);

	private final Function<String, String> authenticateApikey;
	private final Runnable connectedEdgesChanged;

	public OnOpen(//
			Function<String, String> authenticateApikey, //
			Runnable connectedEdgesChanged) {
		this.authenticateApikey = authenticateApikey;
		this.connectedEdgesChanged = connectedEdgesChanged;
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		// Apikey was already validated during the handshake phase (see
		// WebsocketServer.getOnHandshake). Resolve the Edge-ID and set up WsData.
		final var apikey = getAsString(handshakedata, "apikey");
		final WsData wsData = ws.getAttachment();

		// authenticate apikey to resolve Edge-ID; should always succeed since
		// the handshake phase already validated the apikey
		var edgeId = this.authenticateApikey.apply(apikey);
		if (edgeId == null) {
			// Should not happen because apikey was already validated during handshake
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		// announce Edge as online
		wsData.setEdgeId(edgeId);
		wsData.debugLog(this.log, () -> "OPEN " + edgeId);

		this.connectedEdgesChanged.run();

		return null; // No error
	}
}
