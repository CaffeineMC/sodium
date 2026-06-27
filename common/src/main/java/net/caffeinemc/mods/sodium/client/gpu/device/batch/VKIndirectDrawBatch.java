package net.caffeinemc.mods.sodium.client.gpu.device.batch;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.caffeinemc.mods.sodium.client.gpu.device.context.VKIndirectContext;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;

public final class VKIndirectDrawBatch extends MultiDrawBatch {
    private final long pCommands;

    public VKIndirectDrawBatch(int capacity) {
        this.pCommands = MemoryUtil.nmemAlignedAlloc(32, (long) VkDrawIndexedIndirectCommand.SIZEOF * capacity);
        MemoryUtil.memSet(this.pCommands, 0x0, (long) VkDrawIndexedIndirectCommand.SIZEOF * capacity);
        for (int i = 0; i < capacity; i++) {
            MemoryIntrinsics.putInt(this.pCommands + (i * VkDrawIndexedIndirectCommand.SIZEOF) + VkDrawIndexedIndirectCommand.INSTANCECOUNT, 1);
            MemoryIntrinsics.putInt(this.pCommands + (i * VkDrawIndexedIndirectCommand.SIZEOF) + VkDrawIndexedIndirectCommand.FIRSTINSTANCE, 0);
        }
    }

    @Override
    public void put(int size, int elementCount, int baseVertex, long elementOffset) {
        MemoryIntrinsics.putInt(this.pCommands + (size * VkDrawIndexedIndirectCommand.SIZEOF) + VkDrawIndexedIndirectCommand.INDEXCOUNT, elementCount);
        MemoryIntrinsics.putInt(this.pCommands + (size * VkDrawIndexedIndirectCommand.SIZEOF) + VkDrawIndexedIndirectCommand.VERTEXOFFSET, UInt32.uncheckedDowncast(baseVertex));
        MemoryIntrinsics.putInt(this.pCommands + (size * VkDrawIndexedIndirectCommand.SIZEOF) + VkDrawIndexedIndirectCommand.FIRSTINDEX, UInt32.uncheckedDowncast(elementOffset));

        this.updateMaxElementCount(elementCount);
    }

    @Override
    public void draw(DrawContext dc) {
        VKIndirectContext context = (VKIndirectContext) dc;
        MemoryUtil.memCopy(this.pCommands,
                MemoryUtil.memAddress(context.mappedView.data()) + ((long) context.currentOffset * VkDrawIndexedIndirectCommand.SIZEOF),
                (long) this.size * VkDrawIndexedIndirectCommand.SIZEOF);
        GpuBufferSlice commands = context.mappedView.slice().slice(((long) context.currentOffset * VkDrawIndexedIndirectCommand.SIZEOF), (long) this.size * VkDrawIndexedIndirectCommand.SIZEOF);
        context.getPass().drawIndexedIndirect(commands, this.size);
        context.currentOffset += this.size;
    }

    @Override
    public void delete() {
        MemoryUtil.nmemAlignedFree(this.pCommands);
    }
}
