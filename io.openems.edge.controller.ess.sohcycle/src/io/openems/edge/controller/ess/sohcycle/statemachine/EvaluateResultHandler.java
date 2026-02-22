package io.openems.edge.controller.ess.sohcycle.statemachine;

import static io.openems.edge.common.channel.ChannelUtils.setValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.sohcycle.ControllerEssSohCycle;
import io.openems.edge.controller.ess.sohcycle.Utils;

public class EvaluateResultHandler extends StateHandler<StateMachine.State, Context> {
	private static final Logger log = LoggerFactory.getLogger(EvaluateResultHandler.class);

	@Override
	protected StateMachine.State runAndGetNextState(Context context) {
		final var controller = context.getParent();
		final int soc = context.ess.getSoc().orElse(0);

		final Long startWh = context.getMeasurementStartEnergyWh();
		final var endWhValue = context.ess.getActiveDischargeEnergy();

		if (startWh == null || !endWhValue.isDefined()) {
			log.error("{}: Missing measurement data (startWh={}, endWhDefined={}). Aborting.",
					StateMachine.State.EVALUATE_RESULT.getName(), startWh, endWhValue.isDefined());
			return StateMachine.State.ERROR_ABORT;
		}

		final long endWh = endWhValue.get();
		final long measuredCapacityWh = endWh - startWh;

		if (measuredCapacityWh <= 0) {
			log.error("{}: Invalid measured capacity (startWh={}, endWh={}, delta={}). Aborting.",
					StateMachine.State.EVALUATE_RESULT.getName(), startWh, endWh, measuredCapacityWh);
			return StateMachine.State.ERROR_ABORT;
		}

		log.info("{}: SoC={}%, measuredCapacity={} Wh (start={}, end={})", StateMachine.State.EVALUATE_RESULT.getName(),
				soc, measuredCapacityWh, startWh, endWh);
		var sohResult = context.calculateSoh(measuredCapacityWh);
		if (sohResult.isEmpty()) {
			log.error("{}: SoH calculation failed. Aborting.", StateMachine.State.EVALUATE_RESULT.getName());
			return StateMachine.State.ERROR_ABORT;
		}

		var result = sohResult.get();
		setValue(controller, ControllerEssSohCycle.ChannelId.SOH_PERCENT, result.soh());
		setValue(controller, ControllerEssSohCycle.ChannelId.SOH_RAW_DEBUG, Utils.round2(result.sohRaw()));
		setValue(controller, ControllerEssSohCycle.ChannelId.MEASURED_CAPACITY, measuredCapacityWh);
		setValue(controller, ControllerEssSohCycle.ChannelId.IS_MEASURED, true);
		return StateMachine.State.DONE;
	}

}
