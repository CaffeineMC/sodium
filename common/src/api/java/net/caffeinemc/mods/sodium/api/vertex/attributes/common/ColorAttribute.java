package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;

public class ColorAttribute {
    public static void set(long ptr, int color) {
        MemoryIntrinsics.putInt(ptr, color);
    }

    public static int get(long ptr) {
        return MemoryIntrinsics.getInt(ptr);
    }
}
