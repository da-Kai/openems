package io.openems.edge.controller.api.backend.handler;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.common.types.SystemLog;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.backend.ControllerApiBackendImpl;
import io.openems.edge.controller.api.backend.WebsocketClient;

@Component(property = { //
		"entry=" + AuthenticatedRequestHandler.ENTRY_POINT, //
		"org.ops4j.pax.logging.appender.name=Controller.Api.Backend", //
})
public class SubscribeSystemLogJsonApiHandler implements JsonApi, PaxAppender {

	private final Set<WebsocketClient> subscriber = ConcurrentHashMap.newKeySet();
	private final ConcurrentLinkedDeque<SystemLog> logBuffer = new ConcurrentLinkedDeque<>();

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	
	@Activate
	protected void activate() {
		this.executor.scheduleAtFixedRate(this::push, 1, 1, TimeUnit.SECONDS);
	}

	@Deactivate
	protected void deactivate() {
		this.executor.shutdownNow();
	}
	
	private void push() {
		if (this.subscriber.isEmpty()) {
			return;
		}

		final var logs = new ArrayList<SystemLog>();
		SystemLog log;
		while ((log = this.logBuffer.poll()) != null) {
			logs.add(log);
		}
		if (logs.isEmpty()) {
			return;
		}

		final var notification = new SystemLogNotification(logs);
		
		final var iterator = this.subscriber.iterator();
		while (iterator.hasNext()) {
			final var ws = iterator.next();
			if (!ws.sendMessage(notification)) {
				iterator.remove();
			}
		}
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeSystemLogRequest.METHOD, call -> {
			final var webSocket = call.get(ControllerApiBackendImpl.WEBSOCKET_CLIENT_KEY);
			if (webSocket == null) {
				throw new OpenemsException("Websocket is not defined.");
			}
			final var request = SubscribeSystemLogRequest.from(call.getRequest());
			if (request.isSubscribe()) {
				this.subscriber.add(webSocket);
			} else {
				this.subscriber.remove(webSocket);
			}

			return new AuthenticatedRpcResponse(call.getRequest().getId(),
					new GenericJsonrpcResponseSuccess(request.getId()));
		});
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (this.subscriber.isEmpty()) {
			return;
		}

		this.logBuffer.offer(SystemLog.fromPaxLoggingEvent(event));
	}

}