package io.openems.common.websocket;

import com.google.common.base.Stopwatch;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.uri.ResolvedURI;
import io.openems.common.utils.FunctionUtils;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.framing.CloseFrame;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base implementation of a WebSocket client using {@link ResolvedURI}.
 *
 * <p>
 * If the connection is established via a resolved target address (e.g. an IP
 * address), but a host name is still required for HTTP/TLS, this class sets:
 * <ul>
 * <li>the HTTP {@code Host} header</li>
 * <li>the TLS SNI server name via {@link SSLParameters}</li>
 * </ul>
 */
public abstract class WebSocketClient extends org.java_websocket.client.WebSocketClient {

	private final ResolvedURI uri;

	private static Map<String, String> headers(ResolvedURI uri, Map<String, String> headers) {
		final var host = uri.host();
		if (host.isEmpty()) {
			return headers;
		}
		var newHeaders = new HashMap<>(headers);
		newHeaders.put("Host", host.get());
		return newHeaders;
	}

	WebSocketClient(ResolvedURI uri, Draft draft, Map<String, String> headers) {
		super(uri.uri(), draft, WebSocketClient.headers(uri, headers));
		this.uri = uri;
	}

	@Override
	protected void onSetSSLParameters(SSLParameters sslParameters) {
		this.uri.host().ifPresent(hostname -> sslParameters.setServerNames(List.of(new SNIHostName(hostname))));
	}

	private static Field getField(String name) throws NoSuchFieldException {
		Field field = WebSocketClient.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/**
	 * Resets the websocket client state safely.
	 *
	 * <p>
	 * This method closes the current connection and waits for the internal
	 * websocket threads to stop, then clears internal socket/thread references and
	 * resets the draft state. The close phase is bounded by {@code closeTimeoutMs}.
	 *
	 * @param closeTimeoutMs timeout in milliseconds for the close/reset phase
	 * @return elapsed time in milliseconds for a successful reset
	 * @throws TimeoutException if the close/reset phase exceeds {@code closeTimeoutMs}
	 * @throws OpenemsException if reflective access to internal websocket fields fails
	 * @throws InterruptedException if the calling thread is interrupted while waiting
	 */
	public long reset(long closeTimeoutMs) throws TimeoutException, OpenemsException, InterruptedException {
		final Field writeThreadField;
		final Field connectReadThreadField;
		final Field socketField;

		final Thread writeThread;
		final Thread connectReadThread;
		final Draft draft;
		final Socket socket;
		final CountDownLatch closeLatch;
		final WebSocketImpl engine;

		// Get methods and fields via Reflection
		try {
			// WebSocketClient#writeThread
			writeThreadField = getField("writeThread");
			writeThread = (Thread) writeThreadField.get(this);
			// WebSocketClient#connectReadThread
			connectReadThreadField = getField("connectReadThread");
			connectReadThread = (Thread) connectReadThreadField.get(this);
			// WebSocketClient#draft
			draft = (Draft) getField("draft").get(this);
			// WebSocketClient#socket
			socketField = getField("socket");
			socket = (Socket) socketField.get(this);
			// WebSocketClient#closeLatch
			closeLatch = (CountDownLatch) getField("closeLatch").get(this);
			// WebSocketClient#closeLatch
			engine = (WebSocketImpl) getField("engine").get(this);
		} catch (Exception ex) {
			throw new OpenemsException(ex);
		}

		final var timer = Stopwatch.createStarted();

		/*
		 * From here it's a copy of #reset()
		 */
		Thread current = Thread.currentThread();
		if (current == writeThread || current == connectReadThread) {
			throw new IllegalStateException(
					"You cannot initialize a reconnect out of the websocket thread. Use reconnect in another thread to ensure a successful cleanup.");
		}
		var closeSuccess = FunctionUtils.runWithTimeout("WebsocketClient::Close", closeTimeoutMs, () -> {
			try {
				// This socket null check ensures we can reconnect a socket that failed to
				// connect. It's an uncommon edge case, but we want to make sure we support it
				if (engine.getReadyState() == ReadyState.NOT_YET_CONNECTED && socket != null) {
					// Closing the socket when we have not connected prevents the writeThread from
					// hanging on write indefinitely during connection teardown
					socket.close(); // This can deadlock
				}

				// closeBlocking(); -> to reflection
				this.close();
				closeLatch.await(10, TimeUnit.SECONDS);
				// closeBlocking() END
				if (writeThread != null) {
					writeThread.interrupt();
					writeThread.join();
				}
				if (connectReadThread != null) {
					connectReadThread.interrupt();
					connectReadThread.join();
				}
				draft.reset();
				if (socket != null) {
					socket.close(); // This can deadlock
				}
			} catch (InterruptedException ie) {
				this.onError(ie);
				// We are not calling closeConnection() because that would deadlock as well.
			} catch (Exception e) {
				this.onError(e);
				engine.closeConnection(CloseFrame.ABNORMAL_CLOSE, e.getMessage());
			}
		});

		try {
			writeThreadField.set(this, null);
			connectReadThreadField.set(this, null);
			socketField.set(this, null);
		} catch (Exception ex) {
			throw new OpenemsException(ex);
		}

		return switch (closeSuccess) {
			case FunctionUtils.RunWithTimeoutResult.Success() -> timer.elapsed(TimeUnit.MILLISECONDS);
			case FunctionUtils.RunWithTimeoutResult.TimeoutReached(var stacktrace) -> throw  new TimeoutException(stacktrace);
		};
	}

}
