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
