package net.caffeinemc.mods.sodium.client.gpu.device.batch;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMultiDrawIndexedInfoEXT;

public final class VKMultiDrawBatch extends MultiDrawBatch {
    private final long pCommands;

    public VKMultiDrawBatch(int capacity) {
        this.pCommands = MemoryUtil.nmemAlignedAlloc(32, (long) VkMultiDrawIndexedInfoEXT.SIZEOF * capacity);
        MemoryUtil.memSet(this.pCommands, 0x0, (long) VkMultiDrawIndexedInfoEXT.SIZEOF * capacity);
    }

    @Override
    public int getIndexBufferSize() {
        int elements = 0;

        for (var index = 0; index < this.size; index++) {
            elements = Math.max(elements, MemoryIntrinsics.getInt(this.pCommands + ((long) index * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.INDEXCOUNT));
        }

        return elements;
    }

    @Override
    public void put(int size, int elementCount, int baseVertex, long elementOffset) {
        MemoryIntrinsics.putInt(pCommands + (size * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.INDEXCOUNT, elementCount);
        MemoryIntrinsics.putInt(pCommands + (size * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.VERTEXOFFSET, UInt32.uncheckedDowncast(baseVertex));
        MemoryIntrinsics.putInt(pCommands + (size * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.FIRSTINDEX, UInt32.uncheckedDowncast(elementOffset));
    }

    @Override
    public void draw(DrawContext context) {
        context.getPass().multiDrawIndexed(MemoryUtil.memIntBuffer(pCommands, size * 3), 1, 0, size);
    }

    @Override
    public void delete() {
        MemoryUtil.nmemAlignedFree(this.pCommands);
    }
}
