package io.openems.backend.edge.server;

import static io.openems.common.websocket.WebsocketUtils.getAsOptionalString;
import static io.openems.common.websocket.WebsocketUtils.parseRemoteIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.websocket.CommonHttpHeader;
import io.openems.common.websocket.HandshakeData;
import io.openems.common.websocket.WebsocketConnection;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private static final int CLOSE_POLICY_VIOLATION = 1008; // RFC 6455 Policy Violation

	private final Logger log = LoggerFactory.getLogger(OnOpen.class);
	private final Runnable connectedEdgesChanged;

	public OnOpen(Runnable connectedEdgesChanged) {
		this.connectedEdgesChanged = connectedEdgesChanged;
	}

	@Override
	public OpenemsError apply(WebsocketConnection ws, HandshakeData handshakedata) {
		// get apikey from handshake
		final var apikey = getAsOptionalString(handshakedata, CommonHttpHeader.APIKEY).orElse(null);

		var error = this._apply(ws, apikey);
		if (error != null) {
			ws.close(CLOSE_POLICY_VIOLATION, new StringBuilder() //
					.append("Connection to backend failed. Apikey [") //
					.append(apikey).append("]. Remote [") //
					.append(parseRemoteIdentifier(ws, handshakedata)) //
					.append("] Error: ").append(error.name()) //
					.toString());
		}
		return error;
	}

	private OpenemsError _apply(WebsocketConnection ws, String apikey) {
		// get websocket attachment
		WsData wsData = ws.getAttachment();

		// authenticate apikey
		var edgeId = wsData.getEdgeId();
		if (edgeId == null) {
			return OpenemsError.COMMON_AUTHENTICATION_FAILED;
		}

		wsData.debugLog(this.log, () -> "OPEN " + edgeId);

		this.connectedEdgesChanged.run();

		return null; // No error
	}
}
