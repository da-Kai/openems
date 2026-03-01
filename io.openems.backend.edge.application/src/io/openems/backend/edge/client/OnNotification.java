package io.openems.backend.edge.client;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;

import io.openems.backend.common.edge.jsonrpc.UpdateMetadataCache;
import io.openems.backend.metrics.prometheus.PrometheusMetrics;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.EdgeConfigNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.logger.ContextLogger;

public class OnNotification implements io.openems.common.websocket.OnNotification {

	private final Logger log;

	private final String name;
	private final BiConsumer<String, JsonrpcNotification> sendNotificationToEdge;
	private final Consumer<UpdateMetadataCache.Notification> updateCache;

	public OnNotification(//
			String name, //
			BiConsumer<String, JsonrpcNotification> sendNotificationToEdge, //
			Consumer<UpdateMetadataCache.Notification> updateCache) {
		this.name = name;
		this.sendNotificationToEdge = sendNotificationToEdge;
		this.updateCache = updateCache;
		this.log = new ContextLogger(OnNotification.class, name);
	}

	@Override
	public void accept(WebSocket ws, JsonrpcNotification notification) throws OpenemsNamedException {
		try (final var timer = PrometheusMetrics.WEBSOCKET_REQUEST
				.labelValues(this.name, notification.getFullyQualifiedMethod()).startTimer()) {
			switch (notification.getMethod()) {
			case UpdateMetadataCache.Notification.METHOD //
				-> this.updateCache.accept(UpdateMetadataCache.Notification.from(notification));
			case EdgeRpcNotification.METHOD //
				-> this.handleEdgeRpcNotification(EdgeRpcNotification.from(notification));
			default //
				-> this.log.warn("Unhandled Notification: {}", notification);
			}
		}
	}

	/**
	 * Handles a {@link EdgeRpcNotification}.
	 *
	 * @param n the {@link EdgeConfigNotification}
	 */
	private void handleEdgeRpcNotification(EdgeRpcNotification n) {
		final var edgeId = n.getEdgeId();
		final var payload = n.getPayload();
		this.sendNotificationToEdge.accept(edgeId, payload);
	}

}
