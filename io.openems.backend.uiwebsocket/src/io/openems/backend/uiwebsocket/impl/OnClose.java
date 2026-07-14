package io.openems.backend.uiwebsocket.impl;

import org.java_websocket.WebSocket;

public class OnClose implements io.openems.common.websocket.OnClose {

    private final WsSessionRegistry wsSessionRegistry;

    public OnClose(WsSessionRegistry wsSessionRegistry) {
        this.wsSessionRegistry = wsSessionRegistry;
    }

    @Override
    public void accept(WebSocket ws, int code, String reason, boolean remote) {
        WsData wsData = ws.getAttachment();
        if (wsData == null) {
            return;
        }
        this.wsSessionRegistry.unregisterWsData(wsData);
        wsData.dispose();
    }

}