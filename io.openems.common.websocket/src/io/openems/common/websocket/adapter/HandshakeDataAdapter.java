package io.openems.common.websocket.adapter;

import java.util.Iterator;

import org.java_websocket.handshake.Handshakedata;

import io.openems.common.websocket.HandshakeData;

/**
 * Adapter that wraps a {@link Handshakedata} from the java_websocket library as
 * a {@link HandshakeData}.
 */
public class HandshakeDataAdapter implements HandshakeData {

	private final Handshakedata handshake;

	public HandshakeDataAdapter(Handshakedata handshake) {
		this.handshake = handshake;
	}

	@Override
	public Iterator<String> iterateHttpFields() {
		return this.handshake.iterateHttpFields();
	}

	@Override
	public String getFieldValue(String name) {
		return this.handshake.getFieldValue(name);
	}

}
