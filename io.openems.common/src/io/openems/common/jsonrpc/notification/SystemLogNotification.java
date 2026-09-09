package io.openems.common.jsonrpc.notification;

import java.util.List;

import org.ops4j.pax.logging.spi.PaxLoggingEvent;

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
 *     "lines": [
 *     	{@link SystemLog#toJson()}
 *     ]
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
		var json = n.getParams();
		var linesArr = JsonUtils.getAsJsonArray(json, "lines");
		var lines = JsonUtils.toList(linesArr, je -> SystemLog.fromJsonObject(JsonUtils.getAsJsonObject(je)));
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

	/**
	 * Creates a {@link SystemLogNotification} from a list of {@link SystemLog}.
	 * 
	 * @param lines the list of {@link SystemLog}
	 * @return the {@link SystemLogNotification}
	 */
	public static SystemLogNotification fromSystemLogs(List<SystemLog> lines) {
		return new SystemLogNotification(lines);
	}

	public SystemLogNotification(List<SystemLog> lines) {
		super(SystemLogNotification.METHOD);
		this.lines = lines;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.add("lines", JsonUtils.generateJsonArray(this.lines, SystemLog::toJson)) //
				.build();
	}

}
