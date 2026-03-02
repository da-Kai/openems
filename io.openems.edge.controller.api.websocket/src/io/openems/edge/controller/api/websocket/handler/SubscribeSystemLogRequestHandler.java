package io.openems.edge.controller.api.websocket.handler;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.types.SystemLog;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.ControllerApiWebsocket;
import io.openems.edge.controller.api.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.WsData;

@Component(property = { //
		"entry=" + EdgeRpcRequestHandler.ENTRY_POINT, //
		"org.ops4j.pax.logging.appender.name=Controller.Api.Websocket" //
})
public class SubscribeSystemLogRequestHandler implements JsonApi, PaxAppender {

	private static final int LOG_BUFFER_SIZE = 64;

	private final Set<WsData> subscribers = ConcurrentHashMap.newKeySet();
	private final BlockingDeque<SystemLog> logBuffer = new LinkedBlockingDeque<>(LOG_BUFFER_SIZE*2);

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
		if (this.subscribers.isEmpty()) {
			this.logBuffer.clear();
			return;
		}

		final var logs = new ArrayList<SystemLog>();
		this.logBuffer.drainTo(logs, LOG_BUFFER_SIZE);
		if (logs.isEmpty()) {
			return;
		}

		final var notification = new EdgeRpcNotification(ControllerApiWebsocket.EDGE_ID,
				new SystemLogNotification(logs));

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
	public synchronized void doAppend(PaxLoggingEvent event) {
		if (this.subscribers.isEmpty()) {
			return;
		}
		final var sysLog = SystemLog.fromPaxLoggingEvent(event);
		synchronized (this.logBuffer) {
			if (!this.logBuffer.offerLast(sysLog)) {
				this.logBuffer.pollFirst();
				this.logBuffer.offerLast(sysLog);
			}
		}
	}

}
