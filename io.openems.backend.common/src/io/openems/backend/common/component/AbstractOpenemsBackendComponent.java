package io.openems.backend.common.component;

// TODO merge with OpenemsComponent of Edge
public class AbstractOpenemsBackendComponent {

	private final String name;

	/**
	 * Initializes the AbstractOpenemsBackendComponent.
	 *
	 * @param name a descriptive name for this component. Available via
	 *             {@link #getName()}
	 */
	public AbstractOpenemsBackendComponent(String name) {
		this.name = name;
	}

	/**
	 * A descriptive name for this component.
	 *
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}
}
