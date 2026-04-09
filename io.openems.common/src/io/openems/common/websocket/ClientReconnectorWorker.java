package io.openems.common.websocket;

import com.google.common.base.Stopwatch;
import io.openems.common.logger.LazyContextLogger;
import io.openems.common.types.URISet;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.worker.AbstractWorker;
import org.java_websocket.enums.ReadyState;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientReconnectorWorker extends AbstractWorker {

    public record Config(int connectTimeoutSeconds, int maxWaitSeconds, int minWaitSeconds,
                         Consumer<WebsocketReconnectorEvent> onEvent) {

        /**
         * Copies configuration and sets event handler.
         *
         * @param onEvent Event handler to set
         * @return New config instance
         */
        public Config withEventHandler(Consumer<WebsocketReconnectorEvent> onEvent) {
            return new Config(this.connectTimeoutSeconds, this.maxWaitSeconds, this.minWaitSeconds, onEvent);
        }
    }

    public static final ClientReconnectorWorker.Config DEFAULT_CONFIG = new Config(100, 100, 10,
            FunctionUtils::doNothing);

    private static final long CLOSE_TIMEOUT_MILLIS = 60_000L;
    private static final int THREAD_CYCLE_MILLIS = 30_000;

    private final Logger log;
    private final AbstractWebsocketClient<?> parent;
    private final Config config;
    private final URISet serverUris;

    private final List<String> additionalLogInfos = new CopyOnWriteArrayList<>();
    private String debugLog = null;

    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    public ClientReconnectorWorker(AbstractWebsocketClient<?> parent, URISet serverUris, Config config) {
        super(DelayReferencePoint.END_TIME);

        this.parent = parent;
        this.config = config;
        this.serverUris = serverUris;

        this.log = new LazyContextLogger(ClientReconnectorWorker.class, parent::getName);
    }

    @Override
    protected void forever() throws Exception {
        final var parentWs = this.parent.ws.get();
        this.isConnected.set(parentWs != null && parentWs.getReadyState() == ReadyState.OPEN);
        if (this.isConnected.get()) {
            this.debugLog = "ALIVE";
            return;
        }

        final var timer = Stopwatch.createStarted();
        final var retryUris = this.serverUris.resolve();

        this.debugLog = "Reconnecting...";
        this.log.info("Reconnecting Websocket...");

        for (var uri : retryUris) {
            final var ws = this.parent.setupWebsocket(uri);
            try {
                this.log.info("# Connecting WebSocket to '{}'... Blocking[{}s]", uri, this.config.connectTimeoutSeconds());
                this.isConnected.set(ws.connectBlocking(this.config.connectTimeoutSeconds(), TimeUnit.SECONDS));
            } catch (IllegalStateException e) {
                this.log.warn("# Exception while connecting: {}", e.toString());
                this.resetWebSocketClient(ws);
            }


            if (this.isConnected.get()) {
                this.log.info("# Connecting WebSocket to '{}' successfully", uri);
                break;
            }
            this.log.warn("# Connecting WebSocket to '{}' failed", uri);
            this.parent.resetWebsocket();
            TimeUnit.SECONDS.sleep(1);
        }
        timer.stop();

        if (this.isConnected.get()) {
            final var connectionTime = timer.elapsed(TimeUnit.SECONDS);
            this.callEvent(WebsocketReconnectorEvent.CONNECTED);
            this.debugLog = null;
            this.logAndSetDebugInfo(
                    "Connected successfully [" + connectionTime + "s]");
            this.log.info("Connected successfully [{}s]", connectionTime);
        } else {
            this.debugLog = "Connection failed";
            this.log.error("Connection failed");
        }
    }

    private void logAndSetDebugInfo(String message) {
        this.debugLog = message;
        this.log.info(message);
    }

    private void callEvent(WebsocketReconnectorEvent event) {
        try {
            this.config.onEvent().accept(event);
        } catch (RuntimeException ex) {
            this.log.warn("Failed to handle websocket reconnect event '{}'", event.getClass().getSimpleName(), ex);
        }
    }

    /**
     * This method is a copy of {@link WebSocketClient} reset()-method, because the
     * original one may block at the call of 'closeBlocking()' method. It also sets
     * the new attachment from the attachment supplier.
     *
     * @param <T> the type of the attachment
     * @param ws  the {@link WebSocketClient} to reset
     * @throws Exception on error
     */
    protected <T extends WsData> void resetWebSocketClient(WebSocketClient ws) throws Exception {
        this.callEvent(WebsocketReconnectorEvent.RESET_WEBSOCKET_CLIENT);

        try {
            final var time = ws.reset(CLOSE_TIMEOUT_MILLIS);
            this.log.info("Closed websocket connection after {}ms", time);
        } catch (TimeoutException ex) {
            this.log.error("Failed to close socket. Timeout reached. Connection is still open and we continue with a new connection. {}",
                    ex.getMessage());
            this.additionalLogInfos.add("{CLOSE_FAILED: " + ex.getMessage() + "}");
            this.callEvent(WebsocketReconnectorEvent.CLOSE_FAILED);
        }
    }

    @Override
    protected int getCycleTime() {
        final var waitSeconds = ThreadLocalRandom.current().nextInt(this.config.minWaitSeconds, this.config.maxWaitSeconds + 1);
        if (!this.isConnected.get()) {
            this.log.info("Schedule a reconnect in {}s", waitSeconds);
            return waitSeconds * 1_000;
        }
        return THREAD_CYCLE_MILLIS;
    }

    /**
     * Gets some output that is suitable for a continuous Debug log.
     *
     * @return the debug log output or null
     */
    public String debugLog() {
        return Stream.concat(Stream.of(this.debugLog), this.additionalLogInfos.stream()) //
                .filter(Objects::nonNull) //
                .collect(Collectors.joining(", "));
    }

    public enum WebsocketReconnectorEvent {
        RESET_WEBSOCKET_CLIENT, CLOSE_FAILED, CONNECTED
    }

}
