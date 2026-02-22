package io.openems.edge.controller.ess.cycle.statemachine;

import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;

public class FinishedHandler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) {
		final var controller = context.getParent();
		final var config = context.config;

		context.log().info("Current cycle [{}] completed out of [{}] FINISHED", //
				controller.getCompletedCycles(), config.totalCycleNumber());

		return State.FINISHED;
	}
}
