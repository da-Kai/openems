package io.openems.edge.edge2edge.websocket.bridge;

import java.util.concurrent.CompletableFuture;

import io.openems.common.websocket.WebsocketConnection;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> apply(WebsocketConnection ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		throw new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod());
	}

}
