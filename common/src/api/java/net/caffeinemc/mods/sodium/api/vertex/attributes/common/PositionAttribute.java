package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;

public class PositionAttribute {
    public static void put(long ptr, float x, float y, float z) {
        MemoryIntrinsics.putFloat(ptr + 0L, x);
        MemoryIntrinsics.putFloat(ptr + 4L, y);
        MemoryIntrinsics.putFloat(ptr + 8L, z);
    }

    public static float getX(long ptr) {
        return MemoryIntrinsics.getFloat(ptr + 0L);
    }

    public static float getY(long ptr) {
        return MemoryIntrinsics.getFloat(ptr + 4L);
    }

    public static float getZ(long ptr) {
        return MemoryIntrinsics.getFloat(ptr + 8L);
    }
}
