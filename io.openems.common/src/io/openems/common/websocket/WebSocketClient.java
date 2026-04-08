package io.openems.common.websocket;

import io.openems.common.types.ResolvedURI;
import org.java_websocket.drafts.Draft;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Basis-Implementierung eines WebSocketClients mit {@link ResolvedURI}.
 *
 * <p>
 * Wenn die Verbindung über eine aufgelöste Zieladresse (z. B. IP) erfolgt,
 * aber ein Hostname für HTTP/TLS erforderlich ist, setzt diese Klasse:
 * <ul>
 * <li>den HTTP-Header {@code Host}</li>
 * <li>den TLS-SNI-Servernamen über {@link SSLParameters}</li>
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

}
