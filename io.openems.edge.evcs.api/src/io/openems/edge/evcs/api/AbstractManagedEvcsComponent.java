package io.openems.edge.evcs.api;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Abstract Managed EVCS Component.
 * 
 * <p>
 * Includes the logic for the write handler - that is sending the limits
 * depending on the 'send' logic of each implementation. The
 * SET_CHARGE_POWER_LIMIT or SET_CHARGE_POWER_LIMIT_WITH_FILTER Channel are
 * usually set by the evcs Controller.
 *
 * <p>
 * Please ensure to add the event topics at in the properties of the subclass:
 * 
 * <pre>
 * &#64;EventTopics({ //
 *   EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
 * })
 * </pre>
 * 
 * <p>
 * and also call "super.handleEvent(event)" in the subclass:
 * 
 * <pre>
 * &#64;Override
 * public void handleEvent(Event event) {
 * 	super.handleEvent(event);
 * }
 * </pre>
 */
public abstract class AbstractManagedEvcsComponent extends AbstractOpenemsComponent
		implements Evcs, ManagedEvcs, ElectricityMeter, EventHandler {

	protected final WriteHandler writeHandler;
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	protected AbstractManagedEvcsComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.writeHandler = this.createWriteHandler();
	}

	@Override
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);

		Evcs.addCalculatePowerLimitListeners(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.writeHandler.cancelChargePower();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			break;
		}
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	protected WriteHandler createWriteHandler() {
		return new WriteHandler(this);
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}
}
