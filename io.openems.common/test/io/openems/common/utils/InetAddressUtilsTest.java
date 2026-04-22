package io.openems.common.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;

import io.openems.common.exceptions.OpenemsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InetAddressUtilsTest {

	private static final Inet4Address IPv4;
	private static final Inet6Address IPv6;

	static {
		Inet4Address ipv4 = null;
		Inet6Address ipv6 = null;
		try {
			ipv4 = (Inet4Address) Inet4Address.getByName("192.168.1.2");
			ipv6 = (Inet6Address) Inet4Address.getByName("2001:db8::1");
		} catch (UnknownHostException uhe) {
			// Handle exception.
		}
		IPv4 = ipv4;
		IPv6 = ipv6;
	}

	@Test
	public void testParse() throws UnknownHostException {
		assertNull(InetAddressUtils.parseOrNull(null));
		assertNull(InetAddressUtils.parseOrNull(""));
		assertEquals(IPv4, InetAddressUtils.parseOrNull("192.168.1.2"));
		assertEquals(IPv6, InetAddressUtils.parseOrNull("2001:db8::1"));
	}

	@Test
	public void testParseOrError1() throws OpenemsException {
		assertThrows(OpenemsException.class, () -> InetAddressUtils.parseOrError(null));
	}

	@Test
	public void testParseOrError2() throws OpenemsException {
		assertThrows(OpenemsException.class, () -> InetAddressUtils.parseOrError(""));
	}

	@Test
	public void testParseOrError4() throws OpenemsException {
		assertEquals(IPv4, InetAddressUtils.parseOrError("192.168.1.2"));
	}

	@Test
	public void testParseOrError6() throws OpenemsException {
		assertEquals(IPv6, InetAddressUtils.parseOrError("2001:db8::1"));
	}

}
