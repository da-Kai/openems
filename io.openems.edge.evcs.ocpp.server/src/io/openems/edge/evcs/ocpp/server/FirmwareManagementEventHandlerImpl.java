package io.openems.edge.evcs.ocpp.server;

import java.util.UUID;

import org.slf4j.Logger;

import eu.chargetime.ocpp.feature.profile.ServerFirmwareManagementEventHandler;
import eu.chargetime.ocpp.model.firmware.DiagnosticsStatusNotificationConfirmation;
import eu.chargetime.ocpp.model.firmware.DiagnosticsStatusNotificationRequest;
import eu.chargetime.ocpp.model.firmware.FirmwareStatusNotificationConfirmation;
import eu.chargetime.ocpp.model.firmware.FirmwareStatusNotificationRequest;
import io.openems.edge.common.component.OpenemsComponent;

public class FirmwareManagementEventHandlerImpl implements ServerFirmwareManagementEventHandler {

	private final Logger log;

	public FirmwareManagementEventHandlerImpl(OpenemsComponent parent) {
		this.log = OpenemsComponent.getComponentLogger(FirmwareManagementEventHandlerImpl.class, parent);
	}

	@Override
	public DiagnosticsStatusNotificationConfirmation handleDiagnosticsStatusNotificationRequest(UUID sessionIndex,
			DiagnosticsStatusNotificationRequest request) {
		this.log.info("Handle DiagnosticsStatusNotificationRequest");

		return new DiagnosticsStatusNotificationConfirmation();
	}

	@Override
	public FirmwareStatusNotificationConfirmation handleFirmwareStatusNotificationRequest(UUID sessionIndex,
			FirmwareStatusNotificationRequest request) {
		this.log.info("Handle FirmwareStatusNotificationRequest");

		return new FirmwareStatusNotificationConfirmation();
	}
}
