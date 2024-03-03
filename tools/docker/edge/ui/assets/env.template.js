(function(window) {
    window[\"env\"] = window[\"env\"] || {};
  
    // Environment variables
    window[\"env\"][\"websocket\"] = \"ws://${EDGE_HOST:-127.0.0.1}:${EDGE_WEBSOCKET_PORT:-8085}\";
})(this);