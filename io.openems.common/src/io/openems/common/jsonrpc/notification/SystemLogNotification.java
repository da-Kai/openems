package io.openems.common.jsonrpc.notification;

import java.util.ArrayList;
import java.util.List;

import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.SystemLog;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for sending the current system log.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "systemLog",
 *   "params": {
 *     "lines": [{@link SystemLog#toJson()}]
 *   }
 * }
 * </pre>
 */
public class SystemLogNotification extends JsonrpcNotification {

	public static final String METHOD = "systemLog";

	private final List<SystemLog> lines;

	/**
	 * Parses a {@link JsonrpcNotification} to a {@link SystemLogNotification}.
	 *
	 * @param n the {@link JsonrpcNotification}
	 * @return the {@link SystemLogNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static SystemLogNotification from(JsonrpcNotification n) throws OpenemsNamedException {
		var j = n.getParams();
		var linesJson = JsonUtils.getAsJsonArray(j, "lines");
		var lines = new ArrayList<SystemLog>(linesJson.size());
		for (var elem : linesJson) {
			lines.add(SystemLog.fromJsonObject(elem.getAsJsonObject()));
		}
		return new SystemLogNotification(lines);
	}

	/**
	 * Creates a {@link SystemLogNotification} from a {@link PaxLoggingEvent}.
	 *
	 * @param event the {@link PaxLoggingEvent}
	 * @return the {@link SystemLogNotification}
	 */
	public static SystemLogNotification fromPaxLoggingEvent(PaxLoggingEvent event) {
		return new SystemLogNotification(List.of(SystemLog.fromPaxLoggingEvent(event)));
	}

	public SystemLogNotification(List<SystemLog> lines) {
		super(SystemLogNotification.METHOD);
		this.lines = lines;
	}

	@Override
	public JsonObject getParams() {
		var linesJson = new JsonArray(this.lines.size());
		for (var line : this.lines) {
			linesJson.add(line.toJson());
		}
		return JsonUtils.buildJsonObject() //
				.add("lines", linesJson) //
				.build();
	}

}
