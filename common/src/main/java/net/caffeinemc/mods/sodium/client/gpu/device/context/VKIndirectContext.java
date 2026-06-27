package net.caffeinemc.mods.sodium.client.gpu.device.context;


import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.MappableRingBuffer;

public class VKIndirectContext extends VKDrawContext {
    private final MappableRingBuffer ringBuffer;
    public GpuBufferSlice.MappedView mappedView;
    public int currentOffset;

    public VKIndirectContext() {
        this.ringBuffer = new MappableRingBuffer(() -> "Indirect ring buffer", GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_INDIRECT_PARAMETERS, 1_024_000);
        this.mappedView = this.ringBuffer.currentBuffer().map(false, true);
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
}
