package io.openems.edge.controller.api.rest;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.AcceptRateLimit;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.common.handler.ComponentConfigRequestHandler;

public abstract class AbstractRestApi extends AbstractOpenemsComponent
		implements RestApi, Controller, OpenemsComponent {

	public static final boolean DEFAULT_DEBUG_MODE = true;

	protected final ApiWorker apiWorker = new ApiWorker(this.id());

	private final Logger log = OpenemsComponent.getComponentLogger(this);
	private final String implementationName;

	private Server server = null;
	private boolean isDebugModeEnabled = DEFAULT_DEBUG_MODE;

	public AbstractRestApi(String implementationName, io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.implementationName = implementationName;
	}

	/**
	 * Activate the {@link AbstractRestApi}.
	 *
	 * @param context            the {@link ComponentContext}
	 * @param id                 the ID
	 * @param alias              the Alias
	 * @param enabled            enable component?
	 * @param isDebugModeEnabled enable debug mode?
	 * @param apiTimeout         the API timeout in seconds
	 * @param port               the port; if '0', the port is automatically
	 *                           assigned
	 * @param connectionlimit    the connection limit
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			boolean isDebugModeEnabled, int apiTimeout, int port, int connectionlimit) {
		super.activate(context, id, alias, enabled);
		this.isDebugModeEnabled = isDebugModeEnabled;

		if (!this.isEnabled()) {
			// Abort if disabled.
			return;
		}

		this.apiWorker.setTimeoutSeconds(apiTimeout);

		try {
			// Create a server with custom configuration
			this.server = new Server();
			final var httpConfig = new HttpConfiguration();
			httpConfig.setUriCompliance(UriCompliance.from(Set.of(//
					UriCompliance.Violation.SUSPICIOUS_PATH_CHARACTERS, //
					UriCompliance.Violation.ILLEGAL_PATH_CHARACTERS)));
			final var connector = new ServerConnector(this.server, new HttpConnectionFactory(httpConfig));
			connector.setPort(port);
			this.server.addConnector(connector);
			this.server.setHandler(new RestHandler(this));
			this.server.addBean(new AcceptRateLimit(10, 5, TimeUnit.SECONDS, this.server));
			this.server.addBean(new NetworkConnectionLimit(connectionlimit, this.server));
			this.server.start();
			this.log.info("{} started on port [{}].", this.implementationName, port);
			this._setUnableToStart(false);

		} catch (Exception e) {
			this.log.error("Unable to start {} on port [{}]: {}", this.implementationName, port, e.getMessage());
			this._setUnableToStart(true);
		}
		this.getRpcRestHandler().setOnCall(call -> {
			call.put(ComponentConfigRequestHandler.API_WORKER_KEY, this.apiWorker);
		});
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.server != null) {
			try {
				this.server.stop();
			} catch (Exception e) {
				this.log.warn("{} failed to stop: {}", this.implementationName, e.getMessage());
			}
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
	}

	protected boolean isDebugModeEnabled() {
		return this.isDebugModeEnabled;
	}

	/**
	 * Gets the UserService.
	 *
	 * @return the service
	 */
	protected abstract UserService getUserService();

	/**
	 * Gets the ComponentManager.
	 *
	 * @return the service
	 */
	protected abstract ComponentManager getComponentManager();

	/**
	 * Gets the JsonRpcRestHandler.
	 *
	 * @return the service
	 */
	protected abstract JsonRpcRestHandler getRpcRestHandler();

	/**
	 * Gets the AccessMode.
	 *
	 * @return the {@link AccessMode}
	 */
	protected abstract AccessMode getAccessMode();
}
