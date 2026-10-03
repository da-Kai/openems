package io.openems.backend.edge.manager;

import java.util.Objects;
import java.util.UUID;

import io.openems.backend.common.component.AbstractOpenemsBackendComponent;
import io.openems.common.websocket.CommonHttpHeader;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;

import io.openems.common.websocket.AbstractWebsocketServer;

import static io.openems.common.websocket.WebsocketUtils.getAsOptionalString;
import static io.openems.common.websocket.WebsocketUtils.getAsOptionalUuid;

public class WebsocketServer extends AbstractWebsocketServer<WsData> {

	private final Logger log;

	private final EdgeManagerImpl parent;
	private final OnOpen onOpen;
	private final OnRequest onRequest;
	private final OnNotification onNotification;
	private final OnError onError;
	private final OnClose onClose;

	private final String secret;

	public WebsocketServer(EdgeManagerImpl parent, String name, int port, int poolSize, String secret) {
		super(name, port, poolSize);
		this.parent = parent;
		this.log = AbstractOpenemsBackendComponent.getComponentLogger(WebsocketServer.class, parent);
		this.secret = secret;
		this.onOpen = new OnOpen(//
				parent.metadata::generateUpdateMetadataCacheNotification, //
				parent::logInfo);
		this.onRequest = new OnRequest(//
				name, //
				() -> parent.appCenterMetadata, //
				() -> parent.oAuthRegistry, //
				parent.metadata::getEdgeIdForApikey, //
				parent.metadata::getEdgeBySetupPassword, //
				parent.metadata::getEdge, //
				parent::logWarn);
		this.onNotification = new OnNotification(//
				name, //
				() -> parent.eventAdmin, //
				() -> parent.uiWebsocket, //
				() -> parent.timedataManager, //
				parent.metadata::getEdge, //
				parent.systemLogHandler::handleSystemLogNotification, //
				parent::logInfo, //
				parent::logWarn);
		this.onError = new OnError(//
				parent::logWarn);
		this.onClose = new OnClose(//
				parent.metadata::getEdge, //
				parent::logInfo);
	}

	@Override
	protected WsData createWsData(WebSocket ws) {
		return new WsData(ws);
	}

	@Override
	protected WsData onHandshake(WebSocket ws, Draft draft, ClientHandshake request) throws InvalidDataException {
		final var suppliedSecret = getAsOptionalString(request, CommonHttpHeader.EDGE_MANAGER_SECRET).orElse(null);
		final var instanceId = getAsOptionalUuid(request, CommonHttpHeader.INSTANCE_ID).orElse(null);
		if (!Objects.equals(suppliedSecret, secret)) {
			this.log.error("Handshake rejected. Invalid secret [InstanceID={}]", instanceId);
			throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Handshake rejected. Invalid secret");
		}
		this.log.debug("Handshake accepted [InstanceID={}]", instanceId);
		return this.createWsData(ws);
	}


	/**
	 * Is the given Edge online?.
	 *
	 * @param edgeId the Edge-ID
	 * @return true if it is online.
	 */
	public boolean isOnline(String edgeId) {
		return this.getConnections().stream() //
				.map(ws -> (WsData) ws.getAttachment()) //
				.filter(Objects::nonNull) //
				.anyMatch(wsData -> wsData.containsEdgeId(edgeId));
	}

	@Override
	protected OnOpen getOnOpen() {
		return this.onOpen;
	}

	@Override
	protected OnRequest getOnRequest() {
		return this.onRequest;
	}

	@Override
	public OnNotification getOnNotification() {
		return this.onNotification;
	}

	@Override
	protected OnError getOnError() {
		return this.onError;
	}

	@Override
	protected OnClose getOnClose() {
		return this.onClose;
	}

	@Override
	public String debugLog() {
		return super.debugLog();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		this.parent.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		this.parent.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		this.parent.logError(log, message);
	}
}
