package net.caffeinemc.mods.sodium.api.memory;

import org.lwjgl.system.Pointer;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class MemoryIntrinsics {
    private static final Unsafe UNSAFE;
    private static final boolean BITS32 = Pointer.BITS32;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);

            UNSAFE = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Couldn't obtain reference to sun.misc.Unsafe", e);
        }
    }

    /**
     * Copies the number of bytes specified by {@param length} between off-heap buffers {@param src} and {@param dst}.
     * <p>
     * WARNING: This function makes no attempt to verify that the parameters are correct. If you pass invalid pointers
     * or read/write memory outside a buffer, the JVM will likely crash!
     *
     * @param src The source pointer to begin copying from
     * @param dst The destination pointer to begin copying into
     * @param length The number of bytes to copy
     */
    public static void copyMemory(long src, long dst, int length) {
        // This seems to be faster than MemoryUtil.copyMemory in all cases.
        UNSAFE.copyMemory(src, dst, length);
    }

    public static void putInt(long address, int value) {
        UNSAFE.putInt(address, value);
    }

    public static void putFloat(long address, float value) {
        UNSAFE.putFloat(address, value);
    }

    public static void putLong(long address, long value) {
        UNSAFE.putLong(address, value);
    }

    public static void putShort(long address, short value) {
        UNSAFE.putShort(address, value);
    }

    public static void putByte(long address, byte b) {
        UNSAFE.putByte(address, b);
    }

    public static int getInt(long address) {
        return UNSAFE.getInt(address);
    }

    public static float getFloat(long address) {
        return UNSAFE.getFloat(address);
    }

    public static long getLong(long address) {
        return UNSAFE.getLong(address);
    }

    public static short getShort(long address) {
        return UNSAFE.getShort(address);
    }

    public static byte getByte(long address) {
        return UNSAFE.getByte(address);
    }

    public static void putAddress(long address, long value) {
        if (BITS32) {
            UNSAFE.putInt(address, (int) value);
        } else {
            UNSAFE.putLong(address, value);
        }
    }

    public static long getAddress(long address) {
        if (BITS32) {
            return UNSAFE.getInt(address) & 0xFFFF_FFFFL;
        } else {
            return UNSAFE.getLong(address);
        }
    }
}
