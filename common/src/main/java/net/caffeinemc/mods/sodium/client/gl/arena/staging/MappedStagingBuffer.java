package net.caffeinemc.mods.sodium.client.gl.arena.staging;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MappedStagingBuffer implements StagingBuffer {
    private static final float UPLOAD_LIMIT_MARGIN = 0.8f;

    private final MappedBuffer mappedBuffer;
    private final PriorityQueue<CopyCommand> pendingCopies = new ObjectArrayFIFOQueue<>();
    private final PriorityQueue<FencedMemoryRegion> fencedRegions = new ObjectArrayFIFOQueue<>();

    private int start = 0;
    private int pos = 0;

    private final int capacity;
    private int remaining;

    public MappedStagingBuffer(int capacity) {
        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Staging", GpuBuffer.USAGE_COPY_SRC | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST, capacity);
        GpuBufferSlice.MappedView map = buffer.map(false, true);

        this.mappedBuffer = new MappedBuffer(buffer, map, MemoryUtil.memAddress(map.data()));
        this.capacity = capacity;
        this.remaining = this.capacity;
    }

    @Override
    public void enqueueCopy(CommandList commandList, ByteBuffer data, GpuBuffer dst, long writeOffset) {
        int length = data.remaining();

        if (length > this.remaining) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(dst.slice(writeOffset, length), data);

            return;
        }

        int remaining = this.capacity - this.pos;

        // Split the transfer in two if we have enough available memory at the end and start of the buffer
        if (length > remaining) {
            int split = length - remaining;

            this.addTransfer(data.slice(0, remaining), dst, this.pos, writeOffset);
            this.addTransfer(data.slice(remaining, split), dst, 0, writeOffset + remaining);

            this.pos = split;
        } else {
            this.addTransfer(data, dst, this.pos, writeOffset);
            this.pos += length;
        }

        this.remaining -= length;
    }

    private void addTransfer(ByteBuffer data, GpuBuffer dst, long readOffset, long writeOffset) {
        this.mappedBuffer.write(data, (int) readOffset);
        this.pendingCopies.enqueue(new CopyCommand(dst, readOffset, writeOffset, data.remaining()));
    }

    @Override
    public void flush(CommandList commandList) {
        if (this.pendingCopies.isEmpty()) {
            return;
        }

        if (this.pos < this.start) {
            commandList.flushMappedRange(this.mappedBuffer.map, this.start, this.capacity - this.start);
            commandList.flushMappedRange(this.mappedBuffer.map, 0, this.pos);
        } else {
            commandList.flushMappedRange(this.mappedBuffer.map, this.start, this.pos - this.start);
        }

        int bytes = 0;

        for (CopyCommand command : consolidateCopies(this.pendingCopies)) {
            bytes += command.bytes;

            RenderSystem.getDevice().createCommandEncoder().copyToBuffer(this.mappedBuffer.buffer.slice(command.readOffset, command.bytes),
                    command.buffer.slice(command.writeOffset, command.bytes));
        }

        this.fencedRegions.enqueue(new FencedMemoryRegion(RenderSystem.getDevice().createCommandEncoder().createFence(), bytes));

        this.start = this.pos;
    }

    private static List<CopyCommand> consolidateCopies(PriorityQueue<CopyCommand> queue) {
        List<CopyCommand> merged = new ArrayList<>();
        CopyCommand last = null;

        while (!queue.isEmpty()) {
            CopyCommand command = queue.dequeue();

            if (last != null) {
                if (last.buffer == command.buffer &&
                        last.writeOffset + last.bytes == command.writeOffset &&
                        last.readOffset + last.bytes == command.readOffset) {
                    last.bytes += command.bytes;
                    continue;
                }
            }

            merged.add(last = new CopyCommand(command));
        }

        return merged;
    }

    @Override
    public void delete(CommandList commandList) {
        while (!this.fencedRegions.isEmpty()) {
            var region = this.fencedRegions.dequeue();
            var fence = region.fence();
            fence.awaitCompletion(1000);
            fence.close();
        }

        this.mappedBuffer.delete(commandList);
        this.pendingCopies.clear();
    }

    @Override
    public void flip() {
        while (!this.fencedRegions.isEmpty()) {
            var region = this.fencedRegions.first();
            var fence = region.fence();

            if (!fence.awaitCompletion(0)) {
                break;
            }

            fence.close();

            this.fencedRegions.dequeue();
            this.remaining += region.length();
        }
    }

    @Override
    public long getUploadSizeLimit(long frameDuration) {
        return (long) (this.capacity * UPLOAD_LIMIT_MARGIN);
    }

    private static final class CopyCommand {
        private final GpuBuffer buffer;
        private final long readOffset;
        private final long writeOffset;

        private long bytes;

        private CopyCommand(GpuBuffer buffer, long readOffset, long writeOffset, long bytes) {
            this.buffer = buffer;
            this.readOffset = readOffset;
            this.writeOffset = writeOffset;
            this.bytes = bytes;
        }

        public CopyCommand(CopyCommand command) {
            this.buffer = command.buffer;
            this.writeOffset = command.writeOffset;
            this.readOffset = command.readOffset;
            this.bytes = command.bytes;
        }
    }

    private record MappedBuffer(GpuBuffer buffer,
                                GpuBufferSlice.MappedView map, long mapAddr) {
        public void delete(CommandList commandList) {
            this.map.close();
            this.buffer.close();
        }

        public void write(ByteBuffer data, int readOffset) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), this.mapAddr + readOffset, data.remaining());
        }
    }

    private record FencedMemoryRegion(GpuFence fence, int length) {

    }

    @Override
    public String toString() {
        return "Mapped (%s/%s MiB)".formatted(MathUtil.toMib(this.remaining), MathUtil.toMib(this.capacity));
    }
}
