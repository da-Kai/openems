package io.openems.common.websocket;

import io.openems.common.function.BooleanConsumer;
import io.openems.common.utils.FunctionUtils;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.extensions.permessage_deflate.PerMessageDeflateExtension;

import java.net.Proxy;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Configuration parameters for an {@link AbstractWebsocketClient}.
 *
 * @param serverUri         the websocket server URI to resolve
 * @param draft             the websocket draft to use for the connection handshake
 * @param httpHeaders       additional HTTP headers to send during the handshake
 * @param proxy             the proxy to use, or {@code null} if no proxy should be used
 * @param onConnectedChange callback that is notified when the connection state
 *                          changes
 * @param reconnectorConfig configuration for automatic reconnect attempts
 */
public record WebsocketClientParams(URI serverUri, Draft draft, Map<String, String> httpHeaders, Proxy proxy,
                                    BooleanConsumer onConnectedChange,
                                    ClientReconnectorWorker.Config reconnectorConfig) {

	/**
	 * Default value for no custom HTTP headers.
	 */
	public static final Map<String, String> NO_HTTP_HEADERS = Map.of();
	/**
	 * Default value for no proxy.
	 */
	public static final Proxy NO_PROXY = null;
	/**
	 * Default websocket draft with per-message deflate enabled.
	 */
	public static final Draft DEFAULT_DRAFT = new Draft_6455(new PerMessageDeflateExtension());

	/**
	 * Builder for {@link WebsocketClientParams}.
	 *
	 * <p>
	 * Unless overridden, it uses {@link #DEFAULT_DRAFT},
	 * {@link #NO_HTTP_HEADERS}, {@link #NO_PROXY}, a no-op connection-state
	 * callback and {@link ClientReconnectorWorker#DEFAULT_CONFIG}.
	 */
	public static class Builder {
		private final URI serverUri;

		private Draft draft = DEFAULT_DRAFT;
		private Map<String, String> httpHeaders = NO_HTTP_HEADERS;
		private Proxy proxy = NO_PROXY;
		private BooleanConsumer onConnectedChange = FunctionUtils::doNothing;
		private ClientReconnectorWorker.Config reconnectorConfig = ClientReconnectorWorker.DEFAULT_CONFIG;

		/**
		 * Creates a builder from a variable number of candidate websocket server
		 * URIs.
		 *
		 * @param serverUri the server URIs to try
		 */
		public Builder(URI serverUri) {
			this.serverUri = serverUri;
		}

		/**
		 * Sets the websocket draft.
		 *
		 * @param draft the draft to use for the connection handshake
		 * @return this builder
		 */
		public Builder draft(Draft draft) {
			this.draft = draft == null ? DEFAULT_DRAFT : draft;
			return this;
		}

		/**
		 * Sets additional HTTP headers for the websocket handshake.
		 *
		 * @param httpHeaders the HTTP headers to send
		 * @return this builder
		 */
		public Builder httpHeaders(Map<String, String> httpHeaders) {
			this.httpHeaders = httpHeaders == null ? NO_HTTP_HEADERS : Collections.unmodifiableMap(httpHeaders);
			return this;
		}

		/**
		 * Sets the proxy for outgoing websocket connections.
		 *
		 * @param proxy the proxy to use, or {@code null} for no proxy
		 * @return this builder
		 */
		public Builder proxy(Proxy proxy) {
			this.proxy = proxy == null ? NO_PROXY : proxy;
			return this;
		}

		/**
		 * Sets a callback that is notified when the connected state changes.
		 *
		 * @param onConnectedChange the callback to invoke on state changes
		 * @return this builder
		 */
		public Builder onConnectedChange(BooleanConsumer onConnectedChange) {
			this.onConnectedChange = onConnectedChange == null ? FunctionUtils::doNothing : onConnectedChange;
			return this;
		}

		/**
		 * Sets the configuration for automatic reconnect attempts.
		 *
		 * @param reconnectorConfig the reconnect configuration
		 * @return this builder
		 */
		public Builder reconnectorConfig(ClientReconnectorWorker.Config reconnectorConfig) {
			this.reconnectorConfig = reconnectorConfig == null ? ClientReconnectorWorker.DEFAULT_CONFIG : reconnectorConfig;
			return this;
		}

		/**
		 * Builds an immutable {@link WebsocketClientParams} instance from the current
		 * builder state.
		 *
		 * @return the configured websocket client parameters
		 */
		public WebsocketClientParams build() {
			return new WebsocketClientParams(//
					this.serverUri, //
					this.draft, //
					this.httpHeaders, //
					this.proxy, //
					this.onConnectedChange, //
					this.reconnectorConfig);
		}

	}

}
