package io.openems.edge.fenecon.mini.ess.statemachine;

import org.slf4j.Logger;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.fenecon.mini.ess.Config;
import io.openems.edge.fenecon.mini.ess.FeneconMiniEss;

public class Context extends AbstractContext<FeneconMiniEss> {
	
	final Logger log;

	protected final Config config;
	protected final int setActivePower;
	protected final int setReactivePower;

	public Context(FeneconMiniEss parent, Config config, int setActivePower, int setReactivePower) {
		super(parent);
		this.config = config;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
		this.log = OpenemsComponent.getComponentLogger(Context.class, parent);
	}
}