package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;

public class OverlayAttribute {
    public static void set(long ptr, int overlay) {
        MemoryIntrinsics.putInt(ptr + 0, overlay);
    }

    public static int get(long ptr) {
        return MemoryIntrinsics.getInt(ptr);
    }
}
