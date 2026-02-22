package io.openems.edge.controller.ess.cycle.statemachine;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.PowerConstraint;

public class FinalSocHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		final var controller = context.getParent();
		final var ess = context.ess;
		final var config = context.config;

		if (ess.getSoc().get() == config.finalSoc()) {
			return State.FINISHED;
		}

		var power = context.getAcPower(ess, config.hybridEssMode(), config.power());
		if (ess.getSoc().get() > config.finalSoc()) {
			PowerConstraint.apply(ess, controller.id(), ALL, ACTIVE, EQUALS, power);
			context.log().info("DISCHARGE with [{} W]", power);
			return State.FINAL_SOC;
		}

		PowerConstraint.apply(ess, controller.id(), ALL, ACTIVE, EQUALS, -power);
		context.log().info("CHARGE with [{} W]", -power);

		return State.FINAL_SOC;
	}
}
