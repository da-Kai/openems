package io.openems.common.types;

import io.openems.common.uri.ResolvedURI;
import org.junit.jupiter.api.Test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResolvedURITest {

	/**
	 * Make sure {@code :} is not parsed as part of the host. Otherwise, the URI constructor will fail. Important for IPv6 detection in {@link ResolvedURI}
	 */
	@Test
	public void assertUriBehaviour() throws URISyntaxException {
		var errUri = new URI("https://my:example.de");
		assertNull(errUri.getHost());
		assertThrows(URISyntaxException.class, () -> new URI(//
				errUri.getScheme(), errUri.getUserInfo(), "my:example.de", //
				errUri.getPort(), errUri.getPath(), errUri.getQuery(), errUri.getFragment()));
	}

	@Test
	public void domainsTest() throws URISyntaxException, UnknownHostException {
		var host01 = "example.com";
		var ip01 = Inet4Address.getByName("1.1.1.1");
		var uri01 = new ResolvedURI(new URI("https://" + host01), ip01, host01);

		assertEquals(ip01, uri01.ip());
		assertEquals(host01, uri01.host().orElse(null));
		assertEquals("https://" + ip01.getHostAddress(), uri01.uri().toString());

		var host02 = "300.300.300.300";
		var ip02 = Inet4Address.getByName("1.1.1.1");
		var uri02 = new ResolvedURI(new URI("http://" + host02), ip02, host02);

		assertEquals(ip02, uri02.ip());
		assertEquals(host02, uri02.host().orElse(null));
		assertEquals("http://" + ip02.getHostAddress(), uri02.uri().toString());
	}

	@Test
	public void ipv4Test() throws URISyntaxException, UnknownHostException {
		var host01 = "1.1.1.1";
		var ip01 = Inet4Address.getByName(host01);
		var uri01 = new ResolvedURI(new URI("ws://foo.bar/de"), ip01, host01);

		assertEquals(ip01, uri01.ip());
		assertNull(uri01.host().orElse(null));
		assertEquals("ws://" + ip01.getHostAddress() + "/de", uri01.uri().toString());
	}

	@Test
	public void ipv6Test() throws URISyntaxException, UnknownHostException {
		var host01 = "1::5";
		var ip01 = Inet6Address.getByName(host01);
		var uri01 = new ResolvedURI(new URI("ftp://1::5"), ip01, host01);

		assertEquals(ip01, uri01.ip());
		assertNull(uri01.host().orElse(null));
		assertEquals("ftp://[" + ip01.getHostAddress() + "]", uri01.uri().toString());

		var host02 = "1::";
		var ip02 = Inet6Address.getByName(host02);
		var uri02 = new ResolvedURI(new URI("ws://[1::]:8080"), ip02, host02);

		assertEquals(ip02, uri02.ip());
		assertNull(uri02.host().orElse(null));
		assertEquals("ws://[" + ip02.getHostAddress() + "]:8080", uri02.uri().toString());
	}

}
