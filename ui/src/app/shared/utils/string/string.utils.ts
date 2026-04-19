import { ArrayUtils } from "../array/array.utils";
import { AssertionUtils } from "../assertions/assertions.utils";

export namespace StringUtils {

    export const INVALID_STRING = "Passed value is not of type string";
    export type UppercaseString<T extends string> = T extends Uppercase<T> ? T : never;

    export function assertIsString(val: any): asserts val is string {
        AssertionUtils.assertIsDefined(val);
        isValidString(val);
    }

    export function isValidString(val: any): val is string {
        const isString = typeof val === "string";
        if (!isString) {
            throw new Error(INVALID_STRING);
        }
        return isString;
    }

    export function validateStrings(arr: string[] | null): boolean {
        return arr?.every(el => el != null && isValidString(el)) ?? false;
    }

    /**
     * Checks if the value does not occur in array
     *
     * @param val the value
     * @param arr the array
     * @returns true if passed value is not contained by the array
     */
    export function isNotInArr(val: string | null, arr: string[] | null): boolean {
        ArrayUtils.isValidArr(arr);
        StringUtils.isValidString(val);
        StringUtils.validateStrings(arr);
        return arr?.every(el => val != el) ?? true;
    }

    /**
     * Checks if the value does occur in array
     *
     * @param val the value
     * @param arr the array
     * @returns true if passed value is ocurring in the array
     */
    export function isInArr(val: string | null, arr: string[] | null): boolean {
        ArrayUtils.isValidArr(arr);
        StringUtils.isValidString(val);
        StringUtils.validateStrings(arr);
        return arr?.some(el => val == el) ?? false;
    }

    /**
     * Gets the substring between a start and end character
     *
     * @param start the start character
     * @param end the end character
     * @param val the value
     * @returns a string, if valid, else null
     */
    export function getSubstringInBetween(start: string | null, end: string | null, val: string | null): string | null {

        if ((!val || !start || !end) || !(validateStrings([start, end, val]))) {
            throw new Error(INVALID_STRING);
        }

        const startIndex = val.indexOf(start) + 1;
        const endIndex = val.indexOf(end);

        if (startIndex === -1 || !startIndex || endIndex === -1 || !endIndex) {
            return null;
        }

        return val.substring(startIndex, endIndex);
    }

    export function splitBy(value: string | null, key: string): null | string[] {
        if (isValidString(value)) {
            return value.split(key);
        }

        return null;
    }

    /**
     * Checks if the given string is a valid IPv4 address.
     *
     * Each octet must be a number between 0 and 255 (leading zeros are allowed).
     *
     * @param value The string to validate.
     * @returns true if the string is a valid IPv4 address, false otherwise.
     *
     * @example
     * ```typescript
     * isIpv4Address("192.168.0.1");    // true
     * isIpv4Address("001.001.001.001"); // true
     * isIpv4Address("1.1.1.256");       // false
     * isIpv4Address("localhost");       // false
     * ```
     */
    export function isIpv4Address(value: string): boolean {
        return /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/.test(value);
    }

    /**
     * Checks if the given string is a valid IPv6 address.
     *
     * Supports full, abbreviated, and compressed (`::`) notation.
     * Does **not** accept IPv4-mapped IPv6 addresses (e.g. `::ffff:192.168.0.1`).
     *
     * @param value The string to validate.
     * @returns true if the string is a valid IPv6 address, false otherwise.
     *
     * @example
     * ```typescript
     * isIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334"); // true
     * isIpv6Address("::1");                                       // true
     * isIpv6Address("::");                                        // true
     * isIpv6Address("1::2::3");                                   // false
     * isIpv6Address("192.168.0.1");                               // false
     * ```
     */
    export function isIpv6Address(value: string): boolean {
        const hexGroup = "[0-9a-fA-F]{1,4}";
        return new RegExp(
            "^("
            + `(${hexGroup}:){7}${hexGroup}`                   // 1:2:3:4:5:6:7:8       (full)
            + `|(${hexGroup}:){1,7}:`                           // 1::  …  1:2:3:4:5:6:7::
            + `|:((:${hexGroup}){1,7})`                         // ::2  …  ::2:3:4:5:6:7:8
            + "|::"                                              // ::
            + `|(${hexGroup}:){1}(:${hexGroup}){1,6}`           // 1::3  …  1::3:4:5:6:7:8
            + `|(${hexGroup}:){2}(:${hexGroup}){1,5}`           // 1:2::4  …  1:2::4:5:6:7:8
            + `|(${hexGroup}:){3}(:${hexGroup}){1,4}`           // 1:2:3::5  …  1:2:3::5:6:7:8
            + `|(${hexGroup}:){4}(:${hexGroup}){1,3}`           // 1:2:3:4::6  …  1:2:3:4::6:7:8
            + `|(${hexGroup}:){5}(:${hexGroup}){1,2}`           // 1:2:3:4:5::7  …  1:2:3:4:5::7:8
            + `|(${hexGroup}:){6}:${hexGroup}`                  // 1:2:3:4:5:6::8
            + ")$",
        ).test(value);
    }

    export function splitByGetIndexSafely(value: string | null, key: string, index: number): null | string {
        const arr = StringUtils.splitBy(value, key);
        if (arr == null || arr.length == 0) {
            return null;
        }
        return arr[index];
    }

    /**
     * Extracts a numeric suffix from a string.
     * Matches logic: Pattern.compile("[^0-9]+([0-9]+)$")
     *
     * @param val The input string (e.g. "fems123")
     * @returns The number found at the end, or null if format doesn't match.
     */
    export function getTrailingNumber(value: string): number | null {
        if (!isValidString(value)) {
            return null;
        }
        const match = value.match(/[^0-9]+([0-9]+)$/);

        if (match && match.length > 1) {
            return Number.parseInt(match[1]);
        }

        return null;
    }
}
