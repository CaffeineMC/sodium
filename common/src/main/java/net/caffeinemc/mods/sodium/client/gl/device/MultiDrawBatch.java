package net.caffeinemc.mods.sodium.client.gl.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch {
    public final long pElementPointer;
    public final long pElementCount;
    public final long pBaseVertex;

    public int size;
    public boolean isFilled;
    public long maxElementCount;

    public MultiDrawBatch(int capacity) {
        this.pElementPointer = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Pointer.POINTER_SIZE);
        MemoryUtil.memSet(this.pElementPointer, 0x0, (long) capacity * Pointer.POINTER_SIZE);

        this.pElementCount = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        this.pBaseVertex = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
    }

    public void clear() {
        this.size = 0;
        this.isFilled = false;
        this.maxElementCount = 0;
    }

    public void delete() {
        MemoryUtil.nmemAlignedFree(this.pElementPointer);
        MemoryUtil.nmemAlignedFree(this.pElementCount);
        MemoryUtil.nmemAlignedFree(this.pBaseVertex);
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }
}
