package io.openems.edge.fenecon.mini.ess.statemachine;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.statemachine.StateHandler;
import io.openems.edge.fenecon.mini.ess.PcsMode;
import io.openems.edge.fenecon.mini.ess.SetupMode;
import io.openems.edge.fenecon.mini.ess.statemachine.StateMachine.State;

public class ActivateEconomicMode2Handler extends StateHandler<State, Context> {

	@Override
	public State runAndGetNextState(Context context) throws OpenemsNamedException {
		var ess = context.getParent();

		if (ess.getSetupMode() != SetupMode.ON) {
			context.log.info("Wait for Setup-Mode ON");
			return State.ACTIVATE_ECONOMIC_MODE_2;
		}

		context.log.info("Setup-Mode is ON -> Set PCS-Mode ECONOMIC");
		ess.setPcsMode(PcsMode.ECONOMIC);

		return State.ACTIVATE_ECONOMIC_MODE_3;
	}

}
