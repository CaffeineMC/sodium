package net.caffeinemc.mods.sodium.client.gpu.device.batch;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import net.caffeinemc.mods.sodium.client.gpu.device.backend.DrawBackend;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;

public abstract class MultiDrawBatch {
    public int size;
    public boolean isFilled;

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
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }

    public abstract int getIndexBufferSize();

    public abstract void put(int size, int elementCount, int baseVertex, long elementOffset);

    public abstract void draw(DrawContext drawContext);

    public abstract void delete();
}
