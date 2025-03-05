package net.caffeinemc.mods.sodium.client.util.collections;

import net.caffeinemc.mods.sodium.client.util.MathUtil;

import java.util.Arrays;

public class NibbleArray {
    public static byte[] create(int size) {
        return new byte[MathUtil.align(size, 2) >> 1];
    }

    public static int get(byte[] array, int index) {
        int elementIndex = index >> 1;
        int elementShift = (index & 1) << 2;
        return (array[elementIndex] >>> elementShift) & 0xF;
    }

    public static void fill(byte[] array, int value) {
        int nibble = value & 15;
        int element = (nibble << 0) | (nibble << 4);
        Arrays.fill(array, (byte) element);
    }
}
