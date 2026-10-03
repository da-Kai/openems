#!/usr/bin/env bash
set -e

export UI_WEBSOCKET=$(jq -r '.UI_WEBSOCKET' /data/options.json)

echo "Starting OpenEMS UI with UI_WEBSOCKET=${UI_WEBSOCKET}"

exec /init