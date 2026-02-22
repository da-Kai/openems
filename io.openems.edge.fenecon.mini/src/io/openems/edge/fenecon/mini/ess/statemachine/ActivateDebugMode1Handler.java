package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateDebugMode1Handler extends StateHandler<State, Context> {	

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (ess.getSetupMode() != SetupMode.ON) {
			context.log.info("Activate Readonly-Mode: Set Setup-Mode ON");
			ess.setSetupMode(SetupMode.ON);
		}

		return State.ACTIVATE_DEBUG_MODE_2;
	}

}
