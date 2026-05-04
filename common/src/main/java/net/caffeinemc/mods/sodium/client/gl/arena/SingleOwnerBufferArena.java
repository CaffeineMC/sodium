package net.caffeinemc.mods.sodium.client.gl.arena;

import net.caffeinemc.mods.sodium.client.gl.buffer.GlMutableBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SingleOwnerBufferArena extends BufferArena {
    protected SingleOwnerBufferArena(ArenaAggregator parent, GlMutableBuffer initialBuffer, long capacity, int stride) {
        super(parent, initialBuffer, capacity, stride);
    }

    @Override
    protected void handleResizeUploads(CommandList commandList, RegionAllocatorHandle owner, List<PendingUpload> queue, long totalUploadBytes) {
        // resize to the new estimated capacity
        this.resize(commandList, estimateNewCapacityAfterUpload(owner.getFillFractionInv(), queue));

        // Try again to upload any buffers that failed last time
        this.tryUploads(commandList, owner, queue);

        // If we still had failures, something has gone wrong
        if (!queue.isEmpty()) {
            throw new RuntimeException("Failed to upload all buffers");
        }
    }

    @Override
    protected int receiveSegmentsFrom(CommandList commandList, List<BufferSegment> segments, GlMutableBuffer srcBufferObj, RegionAllocatorHandle owner) {
        this.used = owner.used;
        this.usedSegments = segments.size();
        if (this.used > this.capacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        long endOfFreeHead = this.capacity - this.used;
        var pendingCopies = this.buildTransferList(segments, endOfFreeHead);

        long bufferSize = this.capacity * this.stride;
        if (bufferSize >= (1L << 32)) {
            throw new IllegalArgumentException("Maximum arena buffer size is 4 GiB");
        }

        this.executeCopyCommands(commandList, pendingCopies, srcBufferObj, this.arenaBuffer);

        this.finalizeCompactedSegments(endOfFreeHead, segments);

        return pendingCopies.size();
    }

    private void resize(CommandList commandList, long newCapacity) {
        if (this.used > newCapacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        this.checkAssertions();

        long endOfFreeHead = newCapacity - this.used;

        List<BufferSegment> usedSegments = this.getUsedSegments();
        List<PendingBufferCopyCommand> pendingCopies = this.buildTransferList(usedSegments, endOfFreeHead);

        this.transferSegments(commandList, pendingCopies, newCapacity);

        this.finalizeCompactedSegments(endOfFreeHead, usedSegments);
    }

    private ArrayList<BufferSegment> getUsedSegments() {
        ArrayList<BufferSegment> used = new ArrayList<>();
        BufferSegment seg = this.head;

        while (seg != null) {
            BufferSegment next = seg.getNext();

            if (!seg.isFree()) {
                used.add(seg);
            }

            seg = next;
        }

        return used;
    }

    private void transferSegments(CommandList commandList, Collection<PendingBufferCopyCommand> list, long capacity) {
        long bufferSize = capacity * this.stride;
        if (bufferSize >= (1L << 32)) {
            throw new IllegalArgumentException("Maximum arena buffer size is 4 GiB");
        }

        GlMutableBuffer srcBufferObj = this.arenaBuffer;
        GlMutableBuffer dstBufferObj = this.parent.getBufferOfSizeAtLeast(commandList, bufferSize);

        executeCopyCommands(commandList, list, srcBufferObj, dstBufferObj);

        this.parent.releaseBufferForReuse(commandList, srcBufferObj);

        this.arenaBuffer = dstBufferObj;

        // set the capacity using the size of the buffer since it may be larger than the expected capacity due to buffer reuse
        this.capacity = this.arenaBuffer.getSize() / this.stride;
    }

    private void finalizeCompactedSegments(long tail, List<BufferSegment> usedSegments) {
        this.head = BufferSegment.createFreeSegment(this, 0, tail);

        if (usedSegments.isEmpty()) {
            // this.head.setNext(null);
            // TODO: when would this ever happen??
            throw new IllegalStateException("No used segments after compaction");
        } else {
            this.head.setNext(usedSegments.getFirst());
            this.head.getNext().setPrev(this.head);
        }

        this.checkAssertions();
    }



}
