package io.openems.edge.controller.ess.fixstateofcharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.controller.ess.fixstateofcharge.api.EndCondition;

@ObjectClassDefinition(//
		name = "Controller Ess Fix State of Charge", //
		description = "Sets the charge/discharge power for a symmetric energy storage system to reach a specified SoC at a specified time.")
public @interface ConfigFixStateOfCharge {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlFixStateOfCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Is Running", description = "Set controller ON or OFF")
	boolean isRunning() default false;

	@AttributeDefinition(name = "Target SoC", description = "Target State of Charge at a specified time")
	int targetSoc() default 100;

	@AttributeDefinition(name = "Target time specified", description = "If target time specified is set to false, the SoC will be reached as fast as possible.")
	boolean targetTimeSpecified() default false;

	@AttributeDefinition(name = "Target time [YYYY-MM-DDTHH:mm:ssTZD eg. 2023-12-15T13:47:20+01:00]", description = "Target time to reach the targetSoc, e.g. 2023-12-15T13:47:20+01:00.")
	String targetTime();

	@AttributeDefinition(name = "Target time buffer", description = "Buffer minutes to reach the targetSoc before the target time. Prevent Hybrid-Ess from discharging too late (Inverter can only discharge with a defined maximum, divided into PV + Battery).")
	int targetTimeBuffer() default 0;

	@AttributeDefinition(name = "Terminates itself at the end", description = "Set the property isRunning to false after reaching the target SoC and another condition")
	boolean selfTermination() default true;

	@AttributeDefinition(name = "Terminate time buffer in min", description = "Terminate itself after this time buffer. If zero is given, it will terminate instantly.")
	int terminationBuffer() default 0;

	@AttributeDefinition(name = "Terminates itself after separate condition", description = "Terminate itself after separate end condition given by the property endCondition.")
	boolean conditionalTermination() default false;

	@AttributeDefinition(name = "Condition for termination", description = "Terminates itself if the conditionalTermination is true and this end condition was fulfilled.")
	EndCondition endCondition() default EndCondition.CAPACITY_CHANGED;

	@AttributeDefinition(name = "Ess target filter", description = "This is auto-generated by 'Ess-ID'.")
	String ess_target() default "(enabled=true)";

	String webconsole_configurationFactory_nameHint() default "Controller Ess Fix State of Charge [{id}]";

}