package io.openems.backend.edge.server;

import static io.openems.common.websocket.WebsocketUtils.getAsOptionalString;
import static io.openems.common.websocket.WebsocketUtils.getAsOptionalUuid;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.logger.ContextLogger;
import io.openems.common.websocket.CommonHttpHeader;
import io.openems.common.websocket.WebsocketConnection;
import io.openems.common.websocket.adapter.AbstractWebsocketServer;

public final class WebsocketServer extends AbstractWebsocketServer<WsData> {

private final OnOpen onOpen;
private final OnRequest onRequest;
private final OnNotification onNotification;
private final OnError onError;
private final OnClose onClose;
private final Function<String, String> authenticateApikey;

private final Logger log;

public WebsocketServer(String name, int port, int poolSize, //
ction<String, JsonrpcRequest, CompletableFuture<? extends JsonrpcResponseSuccess>> sendRequestToEdgeManager, //
sumer<String, JsonrpcNotification> sendNotificationToEdgeManager, //
ction<String, String> authenticateApikey, //
nable connectedEdgesChanged) {
ame, port, poolSize);
ew ContextLogger(WebsocketServer.class, name);
ticateApikey = authenticateApikey;
Open = new OnOpen(//
nectedEdgesChanged);
Request = new OnRequest(//
ame, //
dRequestToEdgeManager);
Notification = new OnNotification(//
ame, //
dNotificationToEdgeManager);
Error = new OnError(//
Close = new OnClose(//
nectedEdgesChanged);
}

@Override
protected WsData onHandshake(WebSocket ws, Draft draft, ClientHandshake request) throws InvalidDataException {
al var apikey = getAsOptionalString(request, CommonHttpHeader.APIKEY).orElse(null);
al var instanceId = getAsOptionalUuid(request, CommonHttpHeader.INSTANCE_ID).map(UUID::toString).orElse("N/A");
al var edgeId = this.authenticateApikey.apply(apikey);
ull) {
dshake rejected. Invalid Apikey [InstanceID={}]", instanceId);
ew InvalidDataException(CloseFrame.POLICY_VALIDATION, "Handshake rejected. Invalid Apikey");
dshake accepted [InstanceID={}, EdgeID={}]", instanceId, edgeId);
al var wsData = this.createWsData(ws);
 wsData;
}

/**
 * Sends a {@link JsonrpcRequest} to an Edge.
 * 
 * @param edgeId  the Edge-ID
 * @param request the {@link JsonrpcRequest}
 * @return a promise for a successful JSON-RPC Response
 */
public CompletableFuture<JsonrpcResponseSuccess> sendRequestToEdge(String edgeId, JsonrpcRequest request) {
(wsData == null) {
 CompletableFuture.failedFuture(OpenemsError.JSONRPC_SEND_FAILED.exception());
 wsData.send(request);
}

/**
 * Sends a {@link JsonrpcNotification} to an Edge.
 * 
 * @param edgeId       the Edge-ID
 * @param notification the {@link JsonrpcNotification}
 */
public void sendNotificationToEdge(String edgeId, JsonrpcNotification notification) {
(wsData == null) {
; // No connection for this Edge. Ignore.
d(notification);
}

/**
 * Gets the {@link WsData} for the given Edge-ID.
 * 
 * @param edgeId the Edge-ID
 * @return {@link WsData} or null
 */
private WsData getWsDataForEdgeId(String edgeId) {
 this.getConnections().stream() //
t()) //
uals(w.getEdgeId(), edgeId)) //
dFirst().orElse(null);
}

@Override
protected WsData createWsData(WebsocketConnection ws) {
 new WsData(ws);
}

@Override
protected OnOpen getOnOpen() {
 this.onOpen;
}

@Override
protected OnRequest getOnRequest() {
 this.onRequest;
}

@Override
public OnNotification getOnNotification() {
 this.onNotification;
}

@Override
protected OnError getOnError() {
 this.onError;
}

@Override
protected OnClose getOnClose() {
 this.onClose;
}

@Override
protected void logInfo(Logger log, String message) {
fo("[" + this.getName() + "] " + message);
}

@Override
protected void logWarn(Logger log, String message) {
("[" + this.getName() + "] " + message);
}

@Override
protected void logError(Logger log, String message) {
ame() + "] " + message);
}
}
