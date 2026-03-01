package io.openems.edge.common.statemachine;

import io.openems.edge.common.component.OpenemsComponent;

public class AbstractContext<PARENT extends OpenemsComponent> {

	private final PARENT parent;

	/**
	 * Constructs an {@link AbstractContext} without useful logging.
	 */
	public AbstractContext() {
		this(null);
	}

	/**
	 * Constructs an {@link AbstractContext}.
	 *
	 * @param parent the parent {@link OpenemsComponent}. This is used to provide
	 *               useful logging.
	 */
	public AbstractContext(PARENT parent) {
		this.parent = parent;
	}

	/**
	 * Gets the parent {@link OpenemsComponent}.
	 *
	 * @return the parent
	 */
	public PARENT getParent() {
		return this.parent;
	}
}
