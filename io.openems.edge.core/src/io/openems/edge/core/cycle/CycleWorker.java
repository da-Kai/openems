package io.openems.edge.core.cycle;

import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import info.faljse.SDNotify.SDNotify;
import io.openems.common.event.EventBuilder;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.StringUtils;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.Scheduler;

public class CycleWorker extends AbstractWorker {

	private final Logger log = LoggerFactory.getLogger(CycleWorker.class);
	private final CycleImpl parent;

	private final Histogram histogramCycleTime;

	public CycleWorker(CycleImpl parent) {
		this.parent = parent;
		final int maxTrackedValue = (int) (TimeUnit.SECONDS.toMillis(parent.getCycleTime()) * 10);
		this.histogramCycleTime = new Histogram(Math.max(1000, maxTrackedValue), 3);
	}

	@Override
	protected int getCycleTime() {
		return this.parent.getCycleTime();
	}

	private void recordCycleTime(int cycleTimeMs) {
		this.histogramCycleTime.recordValue(cycleTimeMs);
	}

	protected int p99() {
		return (int) this.histogramCycleTime.getValueAtPercentile(99.0);
	}

	protected int p95() {
		return (int) this.histogramCycleTime.getValueAtPercentile(95.0);
	}

	@Override
	protected void forever() {
		// Prepare Cycle-Time measurement
		var stopwatch = Stopwatch.createStarted();

		// Kick Operating System Watchdog
		final boolean hasSocket = StringUtils.isPresent(System.getenv().get("NOTIFY_SOCKET"));
		if (hasSocket && SDNotify.isAvailable()) {
			SDNotify.sendWatchdog();
		}

		try {
			/*
			 * Trigger BEFORE_PROCESS_IMAGE event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);

			/*
			 * Before Controllers start: switch to next process image for each channel
			 */
			this.parent.componentManager.getEnabledComponents().stream() //
					.filter(c -> !(c instanceof Sum)) //
					.forEach(c -> c.channels() //
							.forEach(Channel::nextProcessImage) //
					);
			this.parent.channels().forEach(Channel::nextProcessImage);

			/*
			 * Update the Channels in the Sum-Component.
			 */
			this.parent.sumComponent.updateChannelsBeforeProcessImage();
			this.parent.sumComponent.channels().forEach(Channel::nextProcessImage);

			/*
			 * Trigger AFTER_PROCESS_IMAGE event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE);

			/*
			 * Trigger BEFORE_CONTROLLERS event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS);

			var hasDisabledController = false;

			/*
			 * Execute Schedulers and their Controllers
			 */
			if (this.parent.schedulers.isEmpty()) {
				this.parent.logWarn(this.log, "There are no Schedulers configured!");
			} else {
				for (Scheduler scheduler : this.parent.schedulers) {
					var schedulerControllerIsMissing = false;

					for (String controllerId : scheduler.getControllers()) {
						Controller controller;
						try {
							controller = this.parent.componentManager.getPossiblyDisabledComponent(controllerId);

						} catch (OpenemsNamedException e) {
							this.parent.logWarn(this.log, "Scheduler [" + scheduler.id() + "]: Controller ["
									+ controllerId + "] is missing. " + e.getMessage());
							schedulerControllerIsMissing = true;
							continue;
						}

						if (!controller.isEnabled()) {
							hasDisabledController = true;
							continue;
						}

						try {
							// Execute Controller logic
							controller.run();

							// announce running was ok
							controller._setRunFailed(false);

						} catch (OpenemsNamedException e) {
							this.parent.logWarn(this.log,
									"Error in Controller [" + controller.id() + "]: " + e.getMessage());

							// announce running failed
							controller._setRunFailed(true);

						} catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
							this.parent.logWarn(this.log,
									"Error in Controller [" + controller.id() + "]. " + e.toString());
							this.log.warn(e.toString(), e);

							// announce running failed
							controller._setRunFailed(true);

						} catch (Exception e) {
							this.parent.logWarn(this.log,
									"Error in Controller [" + controller.id() + "]. " + e.toString());
							// announce running failed
							controller._setRunFailed(true);
						}
					}

					// announce Scheduler Controller is missing
					scheduler._setControllerIsMissing(schedulerControllerIsMissing);
				}
			}

			// announce ignoring disabled Controllers.
			this.parent._setIgnoreDisabledController(hasDisabledController);

			/*
			 * Trigger AFTER_CONTROLLERS event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS);

			/*
			 * Trigger BEFORE_WRITE event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE);

			/*
			 * Trigger EXECUTE_WRITE event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE);

			/*
			 * Trigger AFTER_WRITE event
			 */
			EventBuilder.send(this.parent.eventAdmin, EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE);

		} catch (ClassCastException | NullPointerException e) {
			this.parent.logWarn(this.log, "Error in Scheduler. " + e.toString());
			this.log.warn(e.toString(), e);

		} catch (Exception e) {
			this.parent.logWarn(this.log, "Error in Scheduler. " + e.toString());
		}

		// Measure actual Cycle-Time
		stopwatch.stop();
		final var cycleTimeMs = (int) stopwatch.elapsed(TimeUnit.MILLISECONDS);

		this.recordCycleTime(cycleTimeMs);

		this.parent._setMeasuredCycleTime(cycleTimeMs);
		this.parent._setMeasuredCycleTimeP99(this.p99());
		this.parent._setMeasuredCycleTimeP95(this.p95());
	}

}
