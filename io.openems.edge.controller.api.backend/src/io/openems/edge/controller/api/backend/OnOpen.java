package io.openems.edge.controller.api.backend;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.Handshakedata;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.logger.ContextLogger;

public class OnOpen implements io.openems.common.websocket.OnOpen {

	private final Logger log;
	private final ControllerApiBackendImpl parent;

	public OnOpen(ControllerApiBackendImpl parent) {
		this.parent = parent;
		this.log = new ContextLogger(OnOpen.class, parent.id());
	}

	@Override
	public OpenemsError apply(WebSocket ws, Handshakedata handshakedata) {
		this.log.info("Connected to OpenEMS Backend");

		// Immediately send Config
		var config = this.parent.componentManager.getEdgeConfig();
		var message = new EdgeConfigNotification(config);
		this.parent.websocket.sendMessage(message);

		// Send all Channel values
		this.parent.sendChannelValuesWorker.sendValuesOfAllChannelsOnce();

		// Trigger resending data
		this.parent.resendHistoricDataWorker.triggerNextRun();

		return null; // No error
	}

}
