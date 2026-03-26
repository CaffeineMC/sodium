package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;

public class LightAttribute {
    public static void set(long ptr, int light) {
        MemoryIntrinsics.putInt(ptr + 0, light);
    }

    public static int get(long ptr) {
        return MemoryIntrinsics.getInt(ptr);
    }
}
