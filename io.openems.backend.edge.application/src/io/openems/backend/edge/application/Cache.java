package io.openems.backend.edge.application;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.common.function.BooleanConsumer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Cache {

	private final BooleanConsumer onInitializedChange;
	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private volatile Map<String, String> apikeyToEdgeId = Map.of();

	public Cache(BooleanConsumer onInitializedChange) {
		this.onInitializedChange = onInitializedChange;
	}

	protected void update(UpdateMetadataCache.Notification notification) {
		this.apikeyToEdgeId = Map.copyOf(notification.getApikeysToEdgeIds());
		final var init = !this.apikeyToEdgeId.isEmpty();
		if (this.initialized.getAndSet(init) != init) {
			this.onInitializedChange.accept(init);
		}
	}

	/**
	 * Is the {@link Cache} initialized?.
	 * 
	 * @return true if initialized
	 */
	public boolean isInitialized() {
		return this.initialized.get();
	}

	/**
	 * Authenticates an Apikey.
	 * 
	 * @param apikey the Apikey
	 * @return the Edge-ID or null if authentication failed
	 */
	public String authenticateApikey(String apikey) {
		if (apikey == null || apikey.isBlank()) {
			return null;
		}
		return this.apikeyToEdgeId.get(apikey);
	}
}
