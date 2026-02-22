package io.openems.edge.controller.ess.cycle.statemachine;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.PowerConstraint;

public class StartChargeHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		final var ess = context.ess;
		final var config = context.config;

		if (config.maxSoc() == 100 && context.allowedChargePower == 0) {
			return context.waitForChangeState(State.START_CHARGE, State.CONTINUE_WITH_DISCHARGE);
		}

		if (ess.getSoc().get() > config.maxSoc()) {
			return context.waitForChangeState(State.START_CHARGE, State.CONTINUE_WITH_DISCHARGE);
		}

		var power = context.getAcPower(ess, config.hybridEssMode(), config.power());
		PowerConstraint.apply(ess, controller.id(), ALL, ACTIVE, EQUALS, -power);
		
		context.log().info("START CHARGE with [{} W] Current Cycle [{}] out of [{}]",
				-power, controller.getCompletedCycles(), config.totalCycleNumber());

		return State.START_CHARGE;
	}
}
