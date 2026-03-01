package io.openems.edge.controller.ess.sohcycle.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.statemachine.StateHandler;

public class DoneHandler extends StateHandler<StateMachine.State, Context> {
    private static final Logger log = LoggerFactory.getLogger(DoneHandler.class);

    @Override
    protected StateMachine.State runAndGetNextState(Context context) {
        final int soc = context.ess.getSoc().orElse(0);
        log.info("{}: SoC={}%, SoH cycle finished successfully", //
                StateMachine.State.DONE.getName(), soc);
        var controller = context.getParent();
        controller.updateConfigToNotRunning();
        return StateMachine.State.IDLE;
    }
}
