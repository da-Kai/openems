package io.openems.edge.controller.api.websocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.types.SystemLog;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.ControllerApiWebsocket;
import io.openems.edge.controller.api.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.WsData;

@EventTopics(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
@Component(property = { //
		"entry=" + EdgeRpcRequestHandler.ENTRY_POINT, //
		"org.ops4j.pax.logging.appender.name=Controller.Api.Websocket" //
})
public class SubscribeSystemLogRequestHandler implements JsonApi, PaxAppender, EventHandler {

	private final Set<WsData> subscribers = ConcurrentHashMap.newKeySet();
	private final ConcurrentLinkedDeque<SystemLog> cache = new ConcurrentLinkedDeque<>();

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeSystemLogRequest.METHOD, call -> {
			final var request = SubscribeSystemLogRequest.from(call.getRequest());
			final var wsData = call.get(OnRequest.WS_DATA_KEY);

			if (request.isSubscribe()) {
				this.subscribers.add(wsData);
			} else {
				this.subscribers.remove(wsData);
			}

			return new GenericJsonrpcResponseSuccess(request.getId());
		});
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (this.subscribers.isEmpty()) {
			return;
		}
		this.cache.add(SystemLog.fromPaxLoggingEvent(event));
	}

	@Override
	public void handleEvent(Event event) {
		this.sendSystemLogNotifications();
	}

	/**
	 * Drains the cache and sends a {@link SystemLogNotification} with all cached
	 * lines to all subscribers.
	 */
	private void sendSystemLogNotifications() {
		if (this.subscribers.isEmpty() || this.cache.isEmpty()) {
			return;
		}

		final List<SystemLog> lines = new ArrayList<>();
		SystemLog line;
		while ((line = this.cache.poll()) != null) {
			lines.add(line);
		}

		final var notification = new EdgeRpcNotification(ControllerApiWebsocket.EDGE_ID,
				new SystemLogNotification(lines));

		final var iter = this.subscribers.iterator();
		while (iter.hasNext()) {
			final var wsData = iter.next();

			if (wsData.getWebsocket().isFlushAndClose()) {
				iter.remove();
				continue;
			}
			wsData.send(notification);
		}
	}

}
