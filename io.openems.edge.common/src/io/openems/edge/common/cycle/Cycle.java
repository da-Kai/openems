package io.openems.edge.common.cycle;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface Cycle extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.Cycle";
	public static final String SINGLETON_COMPONENT_ID = "_cycle";

	public static final int DEFAULT_CYCLE_TIME = 1000; // in [ms]

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Actual, measured Cycle-Time in [ms].
		 *
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: Integer
		 * </ul>
		 */
		MEASURED_CYCLE_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MILLISECONDS)),

		/**
		 * A configured Controller is not executed because it is disabled.
		 *
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: State
		 * </ul>
		 */
		IGNORE_DISABLED_CONTROLLER(Doc.of(Level.INFO)),

		/**
		 * The 95th percentile of the measured Cycle-Time in [ms]. This means that 95%
		 * of all measured Cycle-Times are below this value.
		 *
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: Integer
		 * </ul>
		 */
		MEASURED_CYCLE_TIME_P95(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS)),

		/**
		 * The 99th percentile of the measured Cycle-Time in [ms]. This means that 99%
		 * of all measured Cycle-Times are below this value.
		 *
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: Integer
		 * </ul>
		 */
		MEASURED_CYCLE_TIME_P99(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLISECONDS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#MEASURED_CYCLE_TIME}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getMeasuredCycleTimeChannel() {
		return this.channel(ChannelId.MEASURED_CYCLE_TIME);
	}

	/**
	 * Gets the Measured Cycle Time in [ms]. See
	 * {@link ChannelId#MEASURED_CYCLE_TIME}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getMeasuredCycleTime() {
		return this.getMeasuredCycleTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MEASURED_CYCLE_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMeasuredCycleTime(Long value) {
		this.getMeasuredCycleTimeChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MEASURED_CYCLE_TIME} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMeasuredCycleTime(long value) {
		this.getMeasuredCycleTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#IGNORE_DISABLED_CONTROLLER}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getIgnoreDisabledControllerChannel() {
		return this.channel(ChannelId.IGNORE_DISABLED_CONTROLLER);
	}

	/**
	 * Gets the Ignore Disabled Controller Info State. See
	 * {@link ChannelId#IGNORE_DISABLED_CONTROLLER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getIgnoreDisabledController() {
		return this.getIgnoreDisabledControllerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#IGNORE_DISABLED_CONTROLLER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setIgnoreDisabledController(boolean value) {
		this.getIgnoreDisabledControllerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MEASURED_CYCLE_TIME_P95}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMeasuredCycleTimeP95Channel() {
		return this.channel(ChannelId.MEASURED_CYCLE_TIME_P95);
	}

	/**
	 * Gets the Measured Cycle Time p95 in [ms]. See
	 * {@link ChannelId#MEASURED_CYCLE_TIME_P95}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMeasuredCycleTimeP95() {
		return this.getMeasuredCycleTimeP95Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MEASURED_CYCLE_TIME_P95} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMeasuredCycleTimeP95(int value) {
		this.getMeasuredCycleTimeP95Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MEASURED_CYCLE_TIME_P99}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMeasuredCycleTimeP99Channel() {
		return this.channel(ChannelId.MEASURED_CYCLE_TIME_P99);
	}

	/**
	 * Gets the Measured Cycle Time p99 in [ms]. See
	 * {@link ChannelId#MEASURED_CYCLE_TIME_P99}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMeasuredCycleTimeP99() {
		return this.getMeasuredCycleTimeP99Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MEASURED_CYCLE_TIME_P99} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMeasuredCycleTimeP99(int value) {
		this.getMeasuredCycleTimeP99Channel().setNextValue(value);
	}

	/**
	 * Gets the duration of one global OpenEMS Cycle in [ms].
	 *
	 * @return the duration in milliseconds
	 */
	public int getCycleTime();

}
