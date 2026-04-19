import { ArrayUtils } from "../array/array.utils";
import { StringUtils } from "./string.utils";

describe("StringUtils", () => {

    describe("+getSubstringInBetween", () => {
        it("valid", () => {
            const msg = "(valid)";
            expect(StringUtils.getSubstringInBetween("(", ")", msg)).toEqual("valid");
        });
        it("valid with white space", () => {
            const msg = "( valid )";
            expect(StringUtils.getSubstringInBetween("(", ")", msg)).toEqual(" valid ");
        });
        it("invalid", () => {
            const msg = "invalid";
            expect(StringUtils.getSubstringInBetween("(", ")", msg)).toEqual(null);
        });
        it("invalid input string, start and end character", () => {
            expect(() => StringUtils.getSubstringInBetween(null, null, null)).toThrow(new Error(StringUtils.INVALID_STRING));
        });
        it("valid string, invalid start and end character", () => {
            const msg = "(valid)";
            expect(() => StringUtils.getSubstringInBetween(null, null, msg)).toThrow(new Error(StringUtils.INVALID_STRING));;
        });
    });

    describe("+isNotInArr", () => {
        it("value is in array", () => {
            expect(StringUtils.isNotInArr("test", ["test", "test2"])).toBeFalse();
        });
        it("value is not in array", () => {
            expect(StringUtils.isNotInArr("test3", ["test", "test2"])).toBeTrue();
        });
        it("value is null", () => {
            expect(() => StringUtils.isNotInArr(null, ["test", "test2"])).toThrow(new Error(StringUtils.INVALID_STRING));
        });
        it("arr is empty", () => {
            expect(StringUtils.isNotInArr("test", [])).toBeTrue();
        });
        it("arr is null", () => {
            expect(() => StringUtils.isNotInArr("test", null)).toThrow(new Error(ArrayUtils.INVALID_ARRAY));
        });
        it("value is null && arr is null", () => {
            expect(() => StringUtils.isNotInArr(null, null)).toThrow(new Error(ArrayUtils.INVALID_ARRAY));
        });
    });

    describe("+isIpv4Address", () => {
        it("should accept valid IPv4 addresses", () => {
            expect(StringUtils.isIpv4Address("001.001.001.001")).toBeTrue();
            expect(StringUtils.isIpv4Address("255.255.255.255")).toBeTrue();
            expect(StringUtils.isIpv4Address("1.1.1.1")).toBeTrue();
            expect(StringUtils.isIpv4Address("0.0.0.0")).toBeTrue();
            expect(StringUtils.isIpv4Address("1.2.20.200")).toBeTrue();
            expect(StringUtils.isIpv4Address("192.168.0.1")).toBeTrue();
            expect(StringUtils.isIpv4Address("10.0.0.1")).toBeTrue();
        });

        it("should reject invalid IPv4 addresses", () => {
            expect(StringUtils.isIpv4Address("1.1.1.1.1")).toBeFalse();
            expect(StringUtils.isIpv4Address("1.1.1.256")).toBeFalse();
            expect(StringUtils.isIpv4Address("1.1.1")).toBeFalse();
            expect(StringUtils.isIpv4Address("999.999.999.999")).toBeFalse();
            expect(StringUtils.isIpv4Address("1.1.1.1.1111")).toBeFalse();
            expect(StringUtils.isIpv4Address("localhost")).toBeFalse();
            expect(StringUtils.isIpv4Address("")).toBeFalse();
            expect(StringUtils.isIpv4Address("abc.def.ghi.jkl")).toBeFalse();
        });
    });

    describe("+isIpv6Address", () => {
        it("should accept valid full IPv6 addresses", () => {
            expect(StringUtils.isIpv6Address("2001:0db8:85a3:0000:0000:8a2e:0370:7334")).toBeTrue();
            expect(StringUtils.isIpv6Address("ABCD:EF01:2345:6789:ABCD:EF01:2345:6789")).toBeTrue();
            expect(StringUtils.isIpv6Address("1:2:3:4:5:6:7:8")).toBeTrue();
        });

        it("should accept valid compressed IPv6 addresses", () => {
            expect(StringUtils.isIpv6Address("::")).toBeTrue();
            expect(StringUtils.isIpv6Address("::1")).toBeTrue();
            expect(StringUtils.isIpv6Address("2001:db8::1")).toBeTrue();
            expect(StringUtils.isIpv6Address("2001:db8:85a3::8a2e:370:7334")).toBeTrue();
            expect(StringUtils.isIpv6Address("fe80::1")).toBeTrue();
            expect(StringUtils.isIpv6Address("1::8")).toBeTrue();
            expect(StringUtils.isIpv6Address("1:2:3:4:5::8")).toBeTrue();
            expect(StringUtils.isIpv6Address("1:2:3:4:5:6::8")).toBeTrue();
            expect(StringUtils.isIpv6Address("1:2:3:4:5:6:7::")).toBeTrue();
            expect(StringUtils.isIpv6Address("::ffff:0:0")).toBeTrue();
        });

        it("should reject invalid IPv6 addresses", () => {
            expect(StringUtils.isIpv6Address("")).toBeFalse();
            expect(StringUtils.isIpv6Address("1:2:3:4:5:6:7:8:9")).toBeFalse();
            expect(StringUtils.isIpv6Address("1::2::3")).toBeFalse();
            expect(StringUtils.isIpv6Address("1:2:3:4:5:6:7")).toBeFalse();
            expect(StringUtils.isIpv6Address("gggg::1")).toBeFalse();
            expect(StringUtils.isIpv6Address("12345::1")).toBeFalse();
            expect(StringUtils.isIpv6Address("192.168.0.1")).toBeFalse();
            expect(StringUtils.isIpv6Address("localhost")).toBeFalse();
            expect(StringUtils.isIpv6Address(":::")).toBeFalse();
        });
    });

    describe("+trailingNumber", () => {
        it("valid trailing number", () => {
            expect(StringUtils.getTrailingNumber("abcd1234")).toEqual(1234);
        });
        it("valid trailing number with special characters", () => {
            expect(StringUtils.getTrailingNumber("user-ID-50")).toEqual(50);
        });
        it("no trailing number", () => {
            expect(StringUtils.getTrailingNumber("abcd")).toEqual(null);
        });
        it("empty string", () => {
            expect(StringUtils.getTrailingNumber("")).toEqual(null);
        });
        it("should return null if the string is purely numeric", () => {
            expect(StringUtils.getTrailingNumber("12345")).toBeNull();
        });
        it("should correctly handle zero", () => {
            expect(StringUtils.getTrailingNumber("Index0")).toBe(0);
        });
    });
});
