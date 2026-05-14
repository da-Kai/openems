package io.openems.backend.uiwebsocket.impl;

import io.openems.common.websocket.WebsocketConnection;

public class OnClose implements io.openems.common.websocket.OnClose {

    @Override
    public void accept(WebsocketConnection ws, int code, String reason, boolean remote) {
        WsData wsData = ws.getAttachment();
        if (wsData == null) {
            return;
        }
        wsData.dispose();
    }

}