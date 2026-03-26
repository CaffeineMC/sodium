package net.caffeinemc.mods.sodium.api.vertex.attributes.common;

import org.joml.Vector2f;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;

public class TextureAttribute {
    public static void put(long ptr, Vector2f vec) {
        put(ptr, vec.x(), vec.y());
    }

    public static void put(long ptr, float u, float v) {
        MemoryIntrinsics.putFloat(ptr + 0, u);
        MemoryIntrinsics.putFloat(ptr + 4, v);
    }

    public static Vector2f get(long ptr) {
        return new Vector2f(getU(ptr), getV(ptr));
    }

    public static float getU(long ptr) {
        return MemoryIntrinsics.getFloat(ptr + 0);
    }

    public static float getV(long ptr) {
        return MemoryIntrinsics.getFloat(ptr + 4);
    }
}
