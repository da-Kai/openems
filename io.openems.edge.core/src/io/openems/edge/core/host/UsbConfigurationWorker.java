package io.openems.edge.core.host;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Hashtable;

import org.slf4j.Logger;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This worker reads the actual USB configuration and stores it in the Host
 * configuration.
 */
public class UsbConfigurationWorker extends AbstractWorker {

	private final Logger log;

	private final HostImpl parent;

	public UsbConfigurationWorker(HostImpl parent) {
		this.parent = parent;
		this.log = OpenemsComponent.getComponentLogger(UsbConfigurationWorker.class, parent);
	}

	@Override
	protected void forever() {
		try {
			var actualUsbConfiguration = this.parent.operatingSystem.getUsbConfiguration();
			var persistedUsbConfiguration = this.parent.config.usbConfiguration();

			if (!actualUsbConfiguration.equals(persistedUsbConfiguration)) {
				this.persistUsbConfiguration(actualUsbConfiguration);
			}

		} catch (OpenemsNamedException | IOException e) {
			this.log.error("Unable to persist actual USB configuration: {}", e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Reconfigure Parent to persist the actual network configuration.
	 *
	 * @param networkConfiguration the actual network configuration
	 * @throws IOException on error
	 */
	private void persistUsbConfiguration(String networkConfiguration) throws IOException {
		var factoryPid = this.parent.serviceFactoryPid();
		final var config = this.parent.cm.getConfiguration(factoryPid, null);
		var properties = config.getProperties();
		if (properties == null) {
			// No 'Host' configuration existing yet -> create new configuration
			properties = new Hashtable<>();
		} else {
			// 'Host' configuration exists -> update configuration
		}
		properties.put("usbConfiguration", networkConfiguration);
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_BY, "Internal UsbConfigurationWorker");
		properties.put(OpenemsConstants.PROPERTY_LAST_CHANGE_AT,
				LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
		config.update(properties);
	}

	@Override
	protected int getCycleTime() {
		return ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN;
	}

}
