package io.openems.common.websocket;

import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.api.TimedataSource;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

public class WebSocketUtils {
		
	private static Logger log = LoggerFactory.getLogger(WebSocketUtils.class);

	public static boolean send(Optional<WebSocket> websocketOpt, JsonObject j) {
		if (!websocketOpt.isPresent()) {
			log.error("Websocket is not available. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
			return false;
		} else {
			return WebSocketUtils.send(websocketOpt.get(), j);
		}
	}

	public static boolean sendNotification(Optional<WebSocket> websocketOpt, LogBehaviour logBehaviour, Notification code,
			Object... params) {
		if (!websocketOpt.isPresent()) {
			log.error("Websocket is not available. Unable to send Notification ["
					+ String.format(code.getMessage(), params) + "]");
			return false;
		} else {
			return WebSocketUtils.sendNotification(websocketOpt.get(), logBehaviour, code, params);
		}
	}

	public static boolean sendNotification(WebSocket websocket, LogBehaviour logBehaviour, Notification code, Object... params) {
		String message = String.format(code.getMessage(), params);
		String logMessage = "Notification [" + code.getValue() + "]: " + message;
		// log message
		if (logBehaviour.equals(LogBehaviour.WRITE_TO_LOG)) {
			switch (code.getStatus()) {
			case INFO:
			case LOG:
			case SUCCESS:
				log.info(logMessage);
				break;
			case ERROR:
				log.error(logMessage);
				break;
			case WARNING:
				log.warn(logMessage);
				break;
			}
		}

		JsonObject j = DefaultMessages.notification(code, message, params);
		return WebSocketUtils.send(websocket, j);
	}

	/**
	 * Send a message to a websocket
	 *
	 * @param j
	 * @return true if successful, otherwise false
	 */
	public static boolean send(WebSocket websocket, JsonObject j) {
		// System.out.println("SEND: websocket["+websocket+"]: " + j.toString());
		try {
			websocket.send(j.toString());
			return true;
		} catch (WebsocketNotConnectedException e) {
			log.error("Websocket is not connected. Unable to send [" + StringUtils.toShortString(j, 100) + "]");
			return false;
		}
	}

	/**
	 * Query history command
	 *
	 * @param j
	 */
	public static JsonObject historicData(JsonArray jMessageId, JsonObject jHistoricData, Optional<Integer> deviceId,
			TimedataSource timedataSource) {
		try {
			String mode = JsonUtils.getAsString(jHistoricData, "mode");
			if (mode.equals("query")) {
				/*
				 * Query historic data
				 */
				int timezoneDiff = JsonUtils.getAsInt(jHistoricData, "timezone");
				ZoneId timezone = ZoneId.ofOffset("", ZoneOffset.ofTotalSeconds(timezoneDiff * -1));
				ZonedDateTime fromDate = JsonUtils.getAsZonedDateTime(jHistoricData, "fromDate", timezone);
				ZonedDateTime toDate = JsonUtils.getAsZonedDateTime(jHistoricData, "toDate", timezone).plusDays(1);
				JsonObject channels = JsonUtils.getAsJsonObject(jHistoricData, "channels");
				// JsonObject kWh = JsonUtils.getAsJsonObject(jQuery, "kWh");
				int days = Period.between(fromDate.toLocalDate(), toDate.toLocalDate()).getDays();
				// TODO: better calculation of sensible resolution
				int resolution = 5 * 60; // 5 Minutes
				if (days > 25) {
					resolution = 24 * 60 * 60; // 1 Day
				} else if (days > 6) {
					resolution = 3 * 60 * 60; // 3 Hours
				} else if (days > 2) {
					resolution = 60 * 60; // 60 Minutes
				}
				JsonArray jData = timedataSource.queryHistoricData(deviceId, fromDate, toDate, channels, resolution);
				// send reply
				return DefaultMessages.historicDataQueryReply(jMessageId, jData);
			}
		} catch (Exception e) {
			log.error("HistoricData Error: ", e);
			e.printStackTrace();
		}
		return new JsonObject();
	}
}
