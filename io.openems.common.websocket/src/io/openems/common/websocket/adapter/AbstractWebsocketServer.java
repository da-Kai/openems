package io.openems.common.websocket.adapter;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.common.websocket.AbstractWebsocket;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.OnCloseHandler;
import io.openems.common.websocket.OnError;
import io.openems.common.websocket.OnErrorHandler;
import io.openems.common.websocket.OnInternalError;
import io.openems.common.websocket.OnMessageHandler;
import io.openems.common.websocket.OnNotification;
import io.openems.common.websocket.OnOpen;
import io.openems.common.websocket.OnOpenHandler;
import io.openems.common.websocket.OnRequest;
import io.openems.common.websocket.WebsocketConnection;
import io.openems.common.websocket.WsData;

public abstract class AbstractWebsocketServer<T extends WsData> extends AbstractWebsocket<T> {

/**
 * Shared {@link ExecutorService}.
 */
private final ThreadPoolExecutor executor;

private final Logger log = LoggerFactory.getLogger(AbstractWebsocketServer.class);
private final int port;
private final WebSocketServer ws;
private final Collection<WebSocket> connections = ConcurrentHashMap.newKeySet();

private boolean isStarted = false;

/**
 * Construct an {@link AbstractWebsocketServer}.
 *
 * @param name     to identify this server
 * @param port     to listen on
 * @param poolSize number of threads dedicated to handle the tasks
 */
protected AbstractWebsocketServer(String name, int port, int poolSize) {
ame);
ewFixedThreadPool(poolSize,
ew ThreadFactoryBuilder().setNameFormat(name + "-%d").build());

new WebSocketServer(new InetSocketAddress(port),
time.getRuntime().availableProcessors(), //
o filter */ List.of(new MyDraft6455()), //
nections) {

Start() {
dshakeBuilder onWebsocketHandshakeReceivedAsServer(//
ClientHandshake request) throws InvalidDataException {
al T wsData = AbstractWebsocketServer.this.onHandshake(ws, draft, request);
t(wsData);
 super.onWebsocketHandshakeReceivedAsServer(ws, draft, request);
Open(WebSocket ws, ClientHandshake handshake) {
nection = new WebsocketConnectionAdapter(ws);
ew OnOpenHandler(//
nection, new HandshakeDataAdapter(handshake), //
Open(), //
, //
dleInternalError));
Message(WebSocket ws, String message) {
nection = new WebsocketConnectionAdapter(ws);
ew OnMessageHandler(//
nection, message, //
Request(), //
Notification(), //
dMessage, //
dleInternalError, //
));
Error(WebSocket ws, Exception ex) {
nection = ws != null ? new WebsocketConnectionAdapter(ws) : null;
ew OnErrorHandler(//
nection, ex, //
Error(), //
dleInternalError));
Close(WebSocket ws, int code, String reason, boolean remote) {
nection = new WebsocketConnectionAdapter(ws);
ew OnCloseHandler(//
nection, code, reason, remote, //
Close(), //
dleInternalError));
 removeConnection(WebSocket ws) {
 AbstractWebsocketServer.this.connections.remove(ws);

 method also does:
connections.isEmpty()) {
terrupt();
 addConnection(WebSocket ws) {
 AbstractWebsocketServer.this.connections.add(ws);

 method also does:
synchronized (connections) {
 this.connections.add(ws);
{
will happen when a new connection gets ready while the server is
stopping.
G_AWAY);
 true;// for consistency sake we will make sure that both onOpen will
Collection<WebSocket> getConnections() {
 AbstractWebsocketServer.this.connections;
to be reused. See
uestions/3229860/what-is-the-meaning-of-so-reuseaddr-setsockopt-option-linux
s a debug log of the current websocket state.
 * 
 * @return the debug log string
 */
public String debugLog() {
ew StringBuilder("[").append(this.getName()).append("] [monitor] ");
d("Connections: ").append(this.connections.size()).append(", ") //
d(ThreadPoolUtils.debugLog(this.executor));
d("NOT STARTED");
 b.toString();
}

/**
 * Returns debug metrics of the current websocket state.
 * 
 * @return the debug metrics
 */
public Map<String, Number> debugMetrics() {
al var metrics = new HashMap<String, Number>();
nections", this.connections.size());
 metrics;
}

@Override
protected OnInternalError getOnInternalError() {
 (t, wsDataString) -> {
dException be //
able to Bind to port [" + this.port + "]");
ew StringBuilder() //
d("OnInternalError for ").append(wsDataString).append(". ") //
d(t.getClass()).append(": ") //
d(t.getMessage()).toString());
dles the WebSocket handshake and creates the connection specific
 * {@link WsData} attachment.
 *
 * <p>
 * Override this method to validate the incoming handshake request and reject it
 * by throwing an {@link InvalidDataException}.
 *
 * @param ws      the current {@link WebSocket} connection
 * @param draft   the negotiated WebSocket {@link Draft}
 * @param request the incoming client handshake
 * @return the {@link WsData} object that is attached to the WebSocket
 * @throws InvalidDataException if the handshake should be rejected
 */
protected T onHandshake(WebSocket ws, Draft draft, ClientHandshake request) throws InvalidDataException {
 this.createWsData(new WebsocketConnectionAdapter(ws));
}

/**
 * Gets all current connections as {@link WebsocketConnection} instances.
 *
 * @return the collection of {@link WebsocketConnection}s
 */
public Collection<WebsocketConnection> getConnections() {
 this.ws.getConnections().stream() //
nectionAdapter::new) //
modifiableList());
}

/**
 * Broadcasts a {@link JsonrpcMessage} to all connected WebSockets.
 *
 * @param message the {@link JsonrpcMessage}
 */
public void broadcastMessage(JsonrpcMessage message) {
this.getConnections()) {
dMessage(ws, message);
{@link JsonrpcMessage} to all connected WebSockets matching a
 * condition.
 *
 * @param message the {@link JsonrpcMessage}
 * @param matcher the matching condition to send the message to
 */
public void broadcastMessage(JsonrpcMessage message, Predicate<T> matcher) {
this.getConnections()) {
t();
tinue;
dMessage(ws, message);
port number that this server listens on.
 *
 * @return The port number.
 */
public int getPort() {
 this.ws.getPort();
}

/**
 * Starts the {@link WebSocketServer}.
 */
@Override
public synchronized void start() {
;
fo(this.log, "Starting websocket server [port=" + this.port + "]");
{@link Runnable} using the shared {@link ExecutorService}.
 *
 * @param command the {@link Runnable}
 */
@Override
protected void execute(Runnable command) {
()) {
 during shutdown
d.run();
d);
{@link WebSocketServer}.
 */
@Override
public synchronized void stop() {
;
 executors
AndAwaitTermination(this.executor, 5);

(tries-- > 0) {
;
ullPointerException | InterruptedException e) {
(this.log,
able to stop websocket server. " + e.getClass().getSimpleName() + ": " + e.getMessage());
(InterruptedException e1) {
ore */
g websocket server failed too often.");
ner/work/openems/openems/io.openems.edge.controller.api.backend/src/io/openems/edge/controller/api/backend/WebsocketClient.java <<'EOF'
package io.openems.edge.controller.api.backend;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.websocket.CommonHttpHeader;
import io.openems.common.websocket.OnClose;
import io.openems.common.websocket.WebsocketConnection;
import io.openems.common.websocket.WebsocketUtils;
import io.openems.common.websocket.WsData;
import io.openems.common.websocket.adapter.AbstractWebsocketClient;
import io.openems.common.websocket.adapter.ClientReconnectorWorker;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

public class WebsocketClient extends AbstractWebsocketClient<WsData> {

private final Logger log = LoggerFactory.getLogger(WebsocketClient.class);

private final ControllerApiBackendImpl parent;
private final OnOpen onOpen;
private final OnNotification onNotification;
private final OnError onError;
private final OnClose onClose;

protected WebsocketClient(ControllerApiBackendImpl parent, String name, URI serverUri,
g, String> httpHeaders, Proxy proxy) {
ame, serverUri, AbstractWebsocketClient.DEFAULT_DRAFT, httpHeaders, proxy, null,
tReconnectorWorker.DEFAULT_CONFIG.withEventHandler(e -> onReconnectEvent(parent, e)));
t = parent;
Open = new OnOpen(parent);
Notification = new OnNotification(parent);
Error = new OnError(parent);
Close = (ws, code, reason, remote) -> {
al var serverUriStr = serverUri.toString();
al var proxyStr = (proxy != AbstractWebsocketClient.NO_PROXY) ? " via Proxy" : "";

EVER_CONNECTED || code == CloseFrame.PROTOCOL_ERROR) {
nect to OpenEMS Backend [{}{}]: {}", //
);
nected from OpenEMS Backend [{}{}]: {}", //
);
t.getUnableToSendChannel().setNextValue(true);
ReconnectEvent(ControllerApiBackendImpl parent,
tReconnectorWorker.WebsocketReconnectorEvent event) {
t == ClientReconnectorWorker.WebsocketReconnectorEvent.CLOSE_FAILED) {
nelUtils.setValue(parent, ControllerApiBackend.ChannelId.CONNECTION_CLOSE_FAILURE, true);
WebsocketHandshakeSent(ClientHandshake request) {
al String systemId = WebsocketUtils //
alString(request, CommonHttpHeader.INSTANCE_ID) //
/A");
fo("Initiating handshake with OpenEMS Backend [InstanceID={}]", systemId);
}

@Override
public OnOpen getOnOpen() {
 this.onOpen;
}

@Override
public BackendOnRequest getOnRequest() {
 this.parent.requestHandler;
}

@Override
public OnNotification getOnNotification() {
 this.onNotification;
}

@Override
public OnError getOnError() {
 this.onError;
}

@Override
public OnClose getOnClose() {
 this.onClose;
}

@Override
protected WsData createWsData(WebsocketConnection ws) {
 new WsData(ws);
}

@Override
protected void logInfo(Logger log, String message) {
t.logInfo(log, message);
}

@Override
protected void logWarn(Logger log, String message) {
t.logWarn(log, message);
}

@Override
protected void logError(Logger log, String message) {
t.logError(log, message);
}

public boolean isConnected() {
 this.ws.isOpen();
}

@Override
protected void execute(Runnable command) {
t.execute(command);
}

/**
 * Schedules a command using the {@link ScheduledExecutorService}.
 *
 * @param command      a {@link Runnable}
 * @param initialDelay the initial delay
 * @param delay        the delay
 * @param unit         the {@link TimeUnit}
 * @return a {@link ScheduledFuture}, or null if Executor is shutting down
 */
protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
it unit) {
 this.parent.scheduleWithFixedDelay(command, initialDelay, delay, unit);
}
}
