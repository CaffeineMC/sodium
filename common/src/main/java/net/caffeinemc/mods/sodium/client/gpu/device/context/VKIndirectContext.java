package net.caffeinemc.mods.sodium.client.gpu.device.context;


import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.MappableRingBuffer;

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

        if (this.currentOffset >= currentSize) {
            recreateRingBuffer(this.currentSize * 2);
        }

        return oldOffset;
    }

    private void recreateRingBuffer(int size) {
        var lastRingBuffer = this.ringBuffer;
        var lastSize = this.currentSize;

        this.currentSize = size;
        this.ringBuffer = new MappableRingBuffer(() -> "Indirect ring buffer", GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_INDIRECT_PARAMETERS | GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_COPY_DST, size);

        if (lastRingBuffer != null) {
            this.mappedView.close();
            RenderSystem.getDevice().createCommandEncoder().copyToBuffer(lastRingBuffer.currentBuffer().slice(), this.ringBuffer.currentBuffer().slice(0, lastSize));
            lastRingBuffer.close(); // This will close the buffer once the frames in flight are done.
        }

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
