package io.openems.common.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import io.openems.common.exceptions.OpenemsException;

public final class InetAddressUtils {

	private InetAddressUtils() {

	}

	public static Optional<Inet4Address> parseIPv4(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			final Inet4Address ipv4 = (Inet4Address) InetAddress.getByName(value.strip());
			return Optional.ofNullable(ipv4);
		} catch (UnknownHostException | ClassCastException e) {
			return Optional.empty();
		}
	}

	public static Optional<Inet6Address> parseIPv6(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			final Inet6Address ipv6 = (Inet6Address) InetAddress.getByName(value.strip());
			return Optional.ofNullable(ipv6);
		} catch (UnknownHostException | ClassCastException e) {
			return Optional.empty();
		}
	}

	/**
	 * Parses a string to an {@link InetAddress} or returns null.
	 *
	 * <p>
	 * See {@link InetAddress#getByName(String)}
	 *
	 * @param value the string value
	 * @return an {@link Optional}&lt;{@link InetAddress}&gt; or null
	 */
	public static Optional<InetAddress> parse(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(InetAddress.getByName(value.strip()));
		} catch (UnknownHostException e) {
			return Optional.empty();
		}
	}

	/**
	 * Parses a string to an {@link InetAddress} or returns null.
	 * 
	 * <p>
	 * See {@link InetAddress#getByName(String)}
	 * 
	 * @param value the string value
	 * @return an {@link InetAddress} or null
	 */
	public static InetAddress parseOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return InetAddress.getByName(value.strip());
		} catch (UnknownHostException e) {
			// handled below
		}
		return null;
	}

	/**
	 * Parses a string to an {@link Inet4Address} or throws an error.
	 * 
	 * <p>
	 * See {@link Inet4Address#getByName(String)}
	 * 
	 * @param value the string value
	 * @return an {@link Inet4Address}
	 * @throws OpenemsException on error
	 */
	public static Inet4Address parseIPv4OrError(String value) throws OpenemsException {
		if (value == null) {
			throw new OpenemsException("IPv4-Address is null");
		}
		if (value.isBlank()) {
			throw new OpenemsException("IPv4-Address is blank");
		}
		try {
			return (Inet4Address) Inet4Address.getByName(value.strip());
		} catch (UnknownHostException | ClassCastException e) {
			throw new OpenemsException("Unable to parse IPv4-Address [" + value + "] " + e.getMessage());
		}
	}

	/**
	 * Parses a string to an {@link Inet6Address} or throws an error.
	 *
	 * <p>
	 * See {@link Inet6Address#getByName(String)}
	 *
	 * @param value the string value
	 * @return an {@link Inet6Address}
	 * @throws OpenemsException on error
	 */
	public static Inet6Address parseIPv6OrError(String value) throws OpenemsException {
		if (value == null) {
			throw new OpenemsException("IPv6-Address is null");
		}
		if (value.isBlank()) {
			throw new OpenemsException("IPv6-Address is blank");
		}
		try {
			return (Inet6Address) Inet6Address.getByName(value.strip());
		} catch (UnknownHostException | ClassCastException e) {
			throw new OpenemsException("Unable to parse IPv6-Address [" + value + "] " + e.getMessage());
		}
	}

	/**
	 * Parses a string to an {@link InetAddress} or throws an error.
	 *
	 * <p>
	 * See {@link InetAddress#getByName(String)}
	 *
	 * @param value the string value
	 * @return an {@link InetAddress}
	 * @throws OpenemsException on error
	 */
	public static InetAddress parseOrError(String value) throws OpenemsException {
		if (value == null) {
			throw new OpenemsException("IP-Address is null");
		}
		if (value.isBlank()) {
			throw new OpenemsException("IP-Address is blank");
		}
		try {
			return InetAddress.getByName(value.strip());
		} catch (UnknownHostException | ClassCastException e) {
			throw new OpenemsException("Unable to parse IP-Address [" + value + "] " + e.getMessage());
		}
	}

}
