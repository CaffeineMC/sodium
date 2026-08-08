package net.caffeinemc.mods.sodium.client.gpu.device.context;


import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;

public class VKIndirectContext extends VKDrawContext {
    private static final int INITIAL_SIZE = 512_000;

    private MappableRingBuffer ringBuffer;
    public GpuBufferSlice.MappedView mappedView;
    private int currentOffset;
    private int currentSize;

    public VKIndirectContext() {
        this.recreateRingBuffer(INITIAL_SIZE);
    }

    public long addCommand(int size) {
        var oldOffset = this.currentOffset;

        this.currentOffset += size;

        if (this.currentOffset > this.currentSize) {
            this.recreateRingBuffer(Math.max(this.currentOffset, this.currentSize * 2));
        }

        return oldOffset;
    }

    private void recreateRingBuffer(int size) {
        var lastRingBuffer = this.ringBuffer;
        var lastMappedView = this.mappedView;
        var lastSize = this.currentSize;

        this.currentSize = size;
        this.ringBuffer = new MappableRingBuffer(() -> "Indirect ring buffer", GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_INDIRECT_PARAMETERS, size);
        this.mappedView = this.ringBuffer.currentBuffer().map(false, true);

        if (lastRingBuffer != null) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(lastMappedView.data()), MemoryUtil.memAddress(this.mappedView.data()), lastSize);
            lastMappedView.close();
            lastRingBuffer.close();
        }
    }

    @Override
    public void rotate() {
        this.mappedView.close();
        this.ringBuffer.rotate();
        this.mappedView = this.ringBuffer.currentBuffer().map(false, true);
        this.currentOffset = 0;
    }

    @Override
    public void delete() {
        this.mappedView.close();
        this.ringBuffer.close();
    }

    @Override
    public void endDraw() {

    }

    @Override
    public String name() {
        return "indirect (sodium)";
    }
}
