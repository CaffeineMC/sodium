package net.caffeinemc.mods.sodium.client.gpu.device.batch;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;

public abstract class MultiDrawBatch {
    public int size;
    public boolean isFilled;

    // Tracks the largest element count across all draw commands in this batch.
    // Updated during put() so getIndexBufferSize() can return it instantly
    // instead of scanning native memory every frame.
    private int maxElementCount;

    public static MultiDrawBatch newBatch(int capacity) {
        return switch (DrawBackend.BACKEND) {
            case OPENGL -> new GLDrawBatch(capacity);
            case VK_MULTIDRAW -> new VKMultiDrawBatch(capacity);
            case VK_INDIRECT -> new VKIndirectDrawBatch(capacity);
        };
    }

    public final void clear() {
        this.size = 0;
        this.isFilled = false;
        this.maxElementCount = 0;
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }


     //Returns the number of elements in the largest draw within this batch.
     //Used by the caller to ensure the shared index buffer is big enough.
    public int getIndexBufferSize() {
        return this.maxElementCount;
    }


     //Called by subclasses when appending a draw command. Keeps the cached
    // maximum up to date so we don't have to scan the full batch later.

    protected final void updateMaxElementCount(int elementCount) {
        if (elementCount > this.maxElementCount) {
            this.maxElementCount = elementCount;
        }
    }

    public abstract void put(int size, int elementCount, int baseVertex, long elementOffset);

    public abstract void draw(DrawContext drawContext);

    public abstract void delete();
}
