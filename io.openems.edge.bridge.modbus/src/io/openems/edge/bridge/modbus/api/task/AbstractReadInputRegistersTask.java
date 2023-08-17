package io.openems.edge.bridge.modbus.api.task;

import java.util.Arrays;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.common.taskmanager.Priority;

@SuppressWarnings("rawtypes")
public abstract class AbstractReadInputRegistersTask<REQUEST extends ModbusRequest, RESPONSE extends ModbusResponse>
		extends AbstractReadTask<REQUEST, RESPONSE, ModbusRegisterElement, InputRegister> {

	public AbstractReadInputRegistersTask(String name, Class<RESPONSE> responseClazz, int startAddress,
			Priority priority, ModbusElement... elements) {
		super(name, responseClazz, ModbusRegisterElement.class, startAddress, priority, elements);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void handleResponse(ModbusRegisterElement element, int position, InputRegister[] response)
			throws OpenemsException {
		element.setInputValue(Arrays.copyOfRange(response, position, position + element.length));
	}

	@Override
	protected int calculateNextPosition(ModbusElement modbusElement, int position) {
		return position + modbusElement.length;
	}
}