package net.caffeinemc.mods.sodium.client.util;

public class BitwiseMath {
    // returns (1) if (a < b), otherwise (0)
    // valid only when (a - b) does not overflow, i.e. when (a) and (b) are within 2^31 of each other
    public static int lessThan(int a, int b) {
        return (a - b) >>> 31;
    }

    // returns (1) if (a > b), otherwise (0)
    // valid only when (b - a) does not overflow, i.e. when (a) and (b) are within 2^31 of each other
    public static int greaterThan(int a, int b) {
        return (b - a) >>> 31;
    }
}