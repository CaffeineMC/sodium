package net.caffeinemc.mods.sodium.client.gpu.device.batch;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public final class GLDrawBatch extends MultiDrawBatch {
    private final long pElementPointer;
    private final long pElementCount;
    private final long pBaseVertex;

    public GLDrawBatch(int capacity) {
        this.pElementPointer = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Pointer.POINTER_SIZE);
        MemoryUtil.memSet(this.pElementPointer, 0x0, (long) capacity * Pointer.POINTER_SIZE);

        this.pElementCount = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        this.pBaseVertex = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
    }

    @Override
    public int getIndexBufferSize() {
        int elements = 0;

        for (var index = 0; index < this.size; index++) {
            elements = Math.max(elements, MemoryIntrinsics.getInt(this.pElementCount + ((long) index * Integer.BYTES)));
        }

        return elements;
    }

    @Override
    public void put(int size, int elementCount, int baseVertex, long elementOffset) {
        MemoryIntrinsics.putInt(pElementCount + (size << 2), UInt32.uncheckedDowncast(elementCount));
        MemoryIntrinsics.putInt(pBaseVertex + (size << 2), UInt32.uncheckedDowncast(baseVertex));

        // * 4 to convert to bytes (the index buffer contains integers)
        MemoryIntrinsics.putAddress(pElementPointer + (size << Pointer.POINTER_SHIFT), elementOffset << 2);
    }

    @Override
    public void draw(DrawContext context) {
        context.getPass().multiDrawIndexed(MemoryUtil.memPointerBuffer(pElementPointer, size),
                MemoryUtil.memIntBuffer(pElementCount, size),
                MemoryUtil.memIntBuffer(pBaseVertex, size), size);
    }

    @Override
    public void delete() {
        MemoryUtil.nmemAlignedFree(this.pElementPointer);
        MemoryUtil.nmemAlignedFree(this.pElementCount);
        MemoryUtil.nmemAlignedFree(this.pBaseVertex);
    }
}
