package net.caffeinemc.mods.sodium.client.gpu.arena;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.gpu.arena.staging.StagingBuffer;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public abstract class BufferArena implements AllocatorBase {
    public static boolean CHECK_ASSERTIONS = false;
    public static boolean CHECK_SEGMENT_ASSERTIONS = true;

    // how many segments we require to be present before we calculate an average size
    public static final int MIN_SEGMENTS_FOR_AVG = 16;
    // growth factor to use when we have too few segments present
    public static final float FEW_SEGMENTS_GROWTH_FACTOR = 1.5f;
    // factor to use when we are allocating with an expected size
    public static final float EXPECTED_SIZE_TARGET_FACTOR = 1.5f;

    final ArenaAggregator parent;
    final StagingBuffer stagingBuffer;
    GpuBuffer arenaBuffer;

    BufferSegment head;

    long capacity;
    long used;
    int usedSegments;

    final int stride;

    protected BufferArena(ArenaAggregator parent, GpuBuffer initialBuffer, long capacity, int stride) {
        this.parent = parent;
        this.stagingBuffer = parent.stagingBuffer;
        this.arenaBuffer = initialBuffer;
        this.capacity = capacity;
        this.stride = stride;

        this.head = BufferSegment.createFreeSegment(this, 0, capacity);
    }

    protected abstract void handleResizeUploads(RegionAllocatorHandle owner, List<PendingUpload> queue, long totalUploadBytes);

    protected abstract int receiveSegmentsFrom(List<BufferSegment> segments, GpuBuffer srcBufferObj, RegionAllocatorHandle owner);

    List<PendingBufferCopyCommand> buildTransferList(List<BufferSegment> usedSegments, long base) {
        List<PendingBufferCopyCommand> pendingCopies = new ArrayList<>();
        PendingBufferCopyCommand currentCopyCommand = null;

        long writeOffset = base;

        for (int i = 0; i < usedSegments.size(); i++) {
            BufferSegment segment = usedSegments.get(i);

            if (currentCopyCommand == null || currentCopyCommand.getReadOffset() + currentCopyCommand.getLength() != segment.getOffset()) {
                if (currentCopyCommand != null) {
                    pendingCopies.add(currentCopyCommand);
                }

                currentCopyCommand = new PendingBufferCopyCommand(segment.getOffset(), writeOffset, segment.getLength());
            } else {
                currentCopyCommand.setLength(currentCopyCommand.getLength() + segment.getLength());
            }

            segment.setOffset(writeOffset);

            if (i + 1 < usedSegments.size()) {
                segment.setNext(usedSegments.get(i + 1));
            } else {
                segment.setNext(null);
            }

            if (i - 1 < 0) {
                segment.setPrev(null);
            } else {
                segment.setPrev(usedSegments.get(i - 1));
            }

            writeOffset += segment.getLength();
        }

        if (currentCopyCommand != null) {
            pendingCopies.add(currentCopyCommand);
        }

        return pendingCopies;
    }

    void executeCopyCommands(Collection<PendingBufferCopyCommand> list, GpuBuffer srcBufferObj, GpuBuffer dstBufferObj) {
        for (PendingBufferCopyCommand cmd : list) {
            RenderSystem.getDevice().createCommandEncoder().copyToBuffer(
                    srcBufferObj.slice(cmd.getReadOffset() * this.stride, cmd.getLength() * this.stride),
                    dstBufferObj.slice(cmd.getWriteOffset() * this.stride, cmd.getLength() * this.stride));
        }
    }

    @Override
    public long getDeviceUsedMemory() {
        return this.used * this.stride;
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return this.capacity * this.stride;
    }

    void updateUsed(long deltaUsed, RegionAllocatorHandle owner) {
        this.used += deltaUsed;
        this.usedSegments += Long.signum(deltaUsed);
    }

    public void registerOwner(RegionAllocatorHandle regionAllocatorHandle) {
    }

    BufferSegment alloc(long size, RegionAllocatorHandle owner, int ownerIndex) {
        this.checkAssertions();

        BufferSegment free = this.takeFree(size);

        if (free == null) {
            return null;
        }

        BufferSegment result;

        // exact fit
        if (free.getLength() == size) {
            free.setOwner(owner, ownerIndex);

            result = free;
        }
        // free space is larger than requested, return new segment at end of free space
        else {
            result = new BufferSegment(this, owner, ownerIndex, free.getEnd() - size, size);
            result.setNext(free.getNext());
            result.setPrev(free);

            if (result.getNext() != null) {
                result.getNext().setPrev(result);
            }

            free.setLength(free.getLength() - size);
            free.setNext(result);
        }

        this.updateUsed(result.getLength(), owner);
        this.checkAssertions();

        return result;
    }

    BufferSegment takeFree(long size) {
        BufferSegment entry = this.head;
        BufferSegment best = null;

        while (entry != null) {
            if (entry.isFree()) {
                if (entry.getLength() == size) {
                    return entry;
                } else if (entry.getLength() >= size) {
                    if (best == null || best.getLength() > entry.getLength()) {
                        best = entry;
                    }
                }
            }

            entry = entry.getNext();
        }

        return best;
    }

    @Override
    public void free(BufferSegment entry) {
        if (entry.isFree()) {
            throw new IllegalStateException("Already freed");
        }

        var owner = entry.getOwner();
        entry.setFree();

        this.updateUsed(-entry.getLength(), owner);

        BufferSegment next = entry.getNext();

        if (next != null && next.isFree()) {
            entry.mergeInto(next);
        }

        BufferSegment prev = entry.getPrev();

        if (prev != null && prev.isFree()) {
            prev.mergeInto(entry);
        }

        this.checkAssertions();
    }

    public abstract void deleteSingleOwner(RegionAllocatorHandle owner);

    @Override
    public boolean isEmpty() {
        return this.used <= 0;
    }

    /**
     * Whether the given owner holds no allocations, which is distinct from the whole arena being empty when the arena is shared.
     *
     * @param owner the owner to check
     * @return true if the owner has no allocations, false otherwise
     */
    abstract boolean isOwnerEmpty(RegionAllocatorHandle owner);

    @Override
    public GpuBuffer getBufferObject() {
        return this.arenaBuffer;
    }

    public boolean upload(RegionAllocatorHandle owner, Stream<PendingUpload> stream) {
        // Record the buffer object before we start any work
        // If the arena needs to re-allocate a buffer, this will allow us to check and return an appropriate flag
        GpuBuffer prevBuffer = this.arenaBuffer;

        // A linked list is used as we'll be randomly removing elements and want O(1) performance
        long totalUploadBytes = 0;
        List<PendingUpload> queue = new LinkedList<>();
        for (var upload : (Iterable<PendingUpload>) stream::iterator) {
            totalUploadBytes += upload.getDataBuffer().getLength();
            queue.add(upload);
        }

        // we need to calculate total owner usage here because uploads will change the owner usage and this way we can avoid recalculating the size of the queue
        var totalUploadSize = totalUploadBytes / this.stride;
        var totalOwnerUsageAfterUploads = totalUploadSize + owner.used;

        // Try to upload all the data into free segments first,
        // but only attempt this if there is enough free space assuming no fragmentation
        if (totalUploadSize < this.capacity - this.used) {
            this.tryUploads(owner, queue);
        }

        // If we weren't able to upload some buffers, they will have been left behind in the queue
        if (!queue.isEmpty()) {
            this.handleResizeUploads(owner, queue, totalOwnerUsageAfterUploads);
        }

        return this.arenaBuffer != prevBuffer;
    }

    static long estimateNewCapacity(int newSegmentCount, float regionFillFractionInv, long requiredNewSize) {
        // the base estimation is to use a growth factor applied to the new required size
        long newCapacity;

        // use average segment size if we have enough segments to make it an accurate value
        if (newSegmentCount >= MIN_SEGMENTS_FOR_AVG) {
            newCapacity = (long) (estimateTotalSize(newSegmentCount, regionFillFractionInv, requiredNewSize) * EXPECTED_SIZE_TARGET_FACTOR);
        } else {
            newCapacity = (long) (requiredNewSize * FEW_SEGMENTS_GROWTH_FACTOR);
        }
        // round up to the next multiple of 4
        // since the new capacity is estimated using non-integers factors, it may end up not being a multiple of 4
        // this causes three separate issues:
        // 1. new segments are always allocated at the end of the free segment, but since the tail is calculated from the capacity,
        //    segments may end up misaligned
        // 2. terrain is rendered with glDrawElementsBaseCount, and since baseCount may not be even, gl_VertexID % 4 would
        //    would not be reliable to detect quads as baseCount is added to the vertex ID
        // 3. misaligned segments on NVIDIA GPUs seem to confuse the driver into splitting quads across workgroups,
        //    possibly due to how the shared index buffer works
        return (newCapacity + 3) & ~3;
    }

    long estimateNewCapacityAfterUpload(float regionFillFractionInv, List<PendingUpload> queue) {
        // Calculate the amount of memory needed for the remaining uploads
        long requiredNewSize = this.getNewRequiredSize(queue);

        int newSegmentCount = this.usedSegments + queue.size();

        return estimateNewCapacity(newSegmentCount, regionFillFractionInv, requiredNewSize);
    }

    static float estimateTotalSize(int newSegmentCount, float regionFillFractionInv, long requiredTotalSize) {
        // find the average segment size after the remaining uploads are allocated
        long averageNewSegmentSize = (requiredTotalSize / newSegmentCount) + 1; // +1 to round up

        // use the average segment size to determine a new capacity, with some overshoot applied for safety
        var expectedSegmentCount = newSegmentCount * regionFillFractionInv;
        return averageNewSegmentSize * expectedSegmentCount;
    }

    long getNewRequiredSize(List<PendingUpload> queue) {
        long remainingUploadBytes = 0;
        for (var upload : queue) {
            remainingUploadBytes += upload.getDataBuffer().getLength();
        }

        // Convert size to elements by dividing by the stride.
        // This doesn't need a ceil since the upload buffers will be at least as big as required and have the same stride.
        long remainingSize = remainingUploadBytes / this.stride;

        // Ask the arena to grow to accommodate the remaining uploads
        // This will force a re-allocation and compaction, which will leave us a continuous free segment
        // for the remaining uploads

        // Re-sizing the arena results in a compaction, so any free space in the arena will be
        // made into one contiguous segment, joined with the new segment of free space we're asking for
        return remainingSize + this.used;
    }

    void tryUploads(RegionAllocatorHandle owner, List<PendingUpload> queue) {
        queue.removeIf(upload -> this.tryUpload(owner, upload));

        // TODO: maybe only do this once rather than repeatedly if we have a move going on
        this.stagingBuffer.flush();
    }

    private boolean tryUpload(RegionAllocatorHandle owner, PendingUpload upload) {
        ByteBuffer data = upload.getDataBuffer().getDirectBuffer();

        int elementCount = data.remaining() / this.stride;

        BufferSegment dst = this.alloc(elementCount, owner, upload.getSegmentOwnerIndex());

        if (dst == null) {
            return false;
        }

        // Copy the data into our staging buffer, then copy it into the arena's buffer
        this.stagingBuffer.enqueueCopy(data, this.arenaBuffer, dst.getOffset() * this.stride);

        upload.setResult(dst);

        return true;
    }

    void checkSegmentAssertions(BufferSegment seg) {
        if (CHECK_SEGMENT_ASSERTIONS || CHECK_ASSERTIONS) {
            if (seg.getOffset() < 0) {
                throw new IllegalStateException("segment.start < 0: out of bounds");
            } else if (seg.getEnd() > this.capacity) {
                throw new IllegalStateException("segment.end > arena.capacity: out of bounds");
            }

            BufferSegment next = seg.getNext();

            if (next != null) {
                if (next.getOffset() < seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start < segment.end: overlapping segments (corrupted)");
                } else if (next.getOffset() > seg.getEnd()) {
                    throw new IllegalStateException("segment.next.start > segment.end: not truly connected (sparsity error)");
                }

                if (next.isFree() && next.getNext() != null) {
                    if (next.getNext().isFree()) {
                        throw new IllegalStateException("segment.free && segment.next.free: not merged consecutive segments");
                    }
                }

                if (next.getPrev() != seg) {
                    throw new IllegalStateException("segment.next.prev != segment: broken linkage");
                }

                if (next == seg) {
                    throw new IllegalStateException("segment.next == segment: infinite loop");
                }

                if (next == this.head) {
                    throw new IllegalStateException("segment.next == arena.head: infinite loop");
                }
            }

            BufferSegment prev = seg.getPrev();

            if (prev != null) {
                if (prev.getEnd() > seg.getOffset()) {
                    throw new IllegalStateException("segment.prev.end > segment.start: overlapping segments (corrupted)");
                } else if (prev.getEnd() < seg.getOffset()) {
                    throw new IllegalStateException("segment.prev.end < segment.start: not truly connected (sparsity error)");
                }

                if (prev.isFree() && prev.getPrev() != null) {
                    if (prev.getPrev().isFree()) {
                        throw new IllegalStateException("segment.free && segment.prev.free: not merged consecutive segments");
                    }
                }

                if (prev.getNext() != seg) {
                    throw new IllegalStateException("segment.prev.next != segment: broken linkage");
                }
            }
        }
    }

    void checkAssertions() {
        if (CHECK_ASSERTIONS) {
            this.checkAssertions0();
        }
    }

    private void checkAssertions0() {
        BufferSegment seg = this.head;
        long used = 0;

        while (seg != null) {
            this.checkSegmentAssertions(seg);

            if (!seg.isFree()) {
                used += seg.getLength();
            }

            seg = seg.getNext();
        }

        if (this.used < 0) {
            throw new IllegalStateException("arena.used < 0: failure to track");
        } else if (this.used > this.capacity) {
            throw new IllegalStateException("arena.used > arena.capacity: failure to track");
        }

        if (this.used != used) {
            throw new IllegalStateException("arena.used is invalid");
        }
    }

    private final Identifier textureId = Identifier.parse("sodium:buffer_debug_" + System.identityHashCode(this));
    public final DynamicTexture texture = new DynamicTexture(this.textureId::toString, 200, 200, true);

    {
        this.texture.getPixels().setPixelABGR(0, 0, 0xFFFFFFFF);
        this.texture.upload();
        Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
    }

    public void renderDebugMap(GuiGraphicsExtractor graphics, int x, int y, int drawWidth, int drawHeight) {
        var image = this.texture.getPixels();
        int width = image.getWidth();
        int height = image.getHeight();

        // draw segments, unused are black, used are colored based on owner id
        var pixelCount = width * height;
        var seg = this.head;
        double pos = 0;
        var sameOwnerSegments = 0;
        while (seg != null) {
            double length = ((double) seg.getLength() / this.capacity) * pixelCount;
            int color;
            if (seg.isFree()) {
                color = 0xFF000000; // black
            } else {
                // color based on owner id
                var owner = seg.getOwner();
                var ownerHash = System.identityHashCode(owner);

                if (seg.getPrev() != null && seg.getPrev().getOwner() == owner) {
                    sameOwnerSegments++;
                } else {
                    sameOwnerSegments = 0;
                }
                color = ColorARGB.fromHSV(
                        (owner.identifier * 0.618033988749895f) % 1.0f,
                        Mth.map(ownerHash & 0xFF, 0, 0xFF, 0.5f, 1.0f),
                        Mth.map(ownerHash >> 8 & 0xFF, 0, 0xFF, 0.5f, 0.8f) +
                                Mth.map(sameOwnerSegments & 0b11, 0, 0b11, 0.0f, 0.2f)
                );
            }

            // draw rects with wrapping
            var lineWidth = width - 1;
            while (length > 0) {
                var yPos = (int) Math.floor(pos / lineWidth);
                var xPos = pos - (yPos * lineWidth);
                var drawLength = Math.min(length, lineWidth - xPos);
                if (yPos >= height || xPos < 0) {
                    break;
                }
                image.fillRect((int) xPos, yPos, (int) Math.ceil(drawLength), 1, color);
                pos += drawLength;
                length -= drawLength;
            }

            seg = seg.getNext();
        }

        this.texture.upload();

        graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureId, x, y, 0, 0, drawWidth, drawHeight, 1, 1, 1, 1);

        int usageOffset = 3;
        graphics.text(Minecraft.getInstance().font, String.format("%d MiB", MathUtil.toMib(this.getDeviceUsedMemory())), x + usageOffset, y + drawHeight - 30, 0xFFFFFFFF);
        graphics.text(Minecraft.getInstance().font, "of", x + usageOffset, y + drawHeight - 20, 0xFFFFFFFF);
        graphics.text(Minecraft.getInstance().font, String.format("%d MiB", MathUtil.toMib(this.getDeviceAllocatedMemory())), x + usageOffset, y + drawHeight - 10, 0xFFFFFFFF);
    }
}
