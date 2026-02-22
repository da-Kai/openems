package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateDebugMode4Handler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (ess.getSetupMode() != SetupMode.OFF) {
			context.log.info("Wait for Setup-Mode OFF");
			return State.ACTIVATE_DEBUG_MODE_4;
		}

		context.log.info("Setup-Mode is OFF");
		return State.GO_WRITE_MODE;
	}

}
