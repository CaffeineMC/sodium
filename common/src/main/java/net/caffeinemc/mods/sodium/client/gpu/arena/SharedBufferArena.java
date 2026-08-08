package net.caffeinemc.mods.sodium.client.gpu.arena;

import com.mojang.renderpearl.api.buffers.GpuBuffer;

import java.util.ArrayList;
import java.util.List;

public class SharedBufferArena extends DefragmentingBufferArena implements SizedTreeMap.Sized {
    private static final int TRANSFER_ABORTED = -1;
    final SizedTreeMap<RegionAllocatorHandle> ownersByUsed = new SizedTreeMap<>();
    private boolean isEmptying = false;
    private SharedBufferArena compactionPair = null;

    private final int identifier;
    private static int nextIdentifier = 1;

    protected SharedBufferArena(ArenaAggregator allocator, GpuBuffer initialBuffer, long capacity, int stride) {
        super(allocator, initialBuffer, capacity, stride);
        this.identifier = nextIdentifier++;
    }

    @Override
    public long getSize() {
        return this.getBiggestFreeSegmentSize();
    }

    @Override
    public long getIdentifier() {
        return this.identifier;
    }

    @Override
    public void deleteSingleOwner(RegionAllocatorHandle owner) {
        // don't delete the buffer when a single owner is deleted
        this.removeOwner(owner);
    }

    public void deleteShared() {
        this.arenaBuffer.close();

        if (this.compactionPair != null) {
            this.compactionPair.compactionPair = null;
            this.compactionPair = null;
        }
    }

    public long getUsed() {
        return this.used;
    }

    public long getCapacity() {
        return this.capacity;
    }

    public long getFree() {
        return this.capacity - this.used;
    }

    public boolean isEmpty() {
        // NOTE: the arena is only empty when there are no owners, they may currently have no data allocated
        return this.ownersByUsed.isEmpty();
    }

    @Override
    boolean isOwnerEmpty(RegionAllocatorHandle owner) {
        // an individual owner is empty when it holds no segments
        return owner.used <= 0;
    }

    public boolean isEmptying() {
        return this.isEmptying;
    }

    public void setEmptying(boolean emptying) {
        this.isEmptying = emptying;
        if (!emptying && this.compactionPair != null) {
            this.compactionPair.compactionPair = null;
            this.compactionPair = null;
        }
    }

    public boolean isCompactionTarget() {
        return this.compactionPair != null && !this.isEmptying;
    }

    public boolean isNotCompacting() {
        return this.compactionPair == null;
    }

    public void makeCompactionSource(SharedBufferArena targetArena) {
        if (this.compactionPair != null || targetArena.compactionPair != null) {
            throw new IllegalStateException("One of the arenas is already part of a compaction pair");
        }
        this.isEmptying = true;
        this.compactionPair = targetArena;
        targetArena.compactionPair = this;
    }

    protected void addOwner(RegionAllocatorHandle owner) {
        if (this.ownersByUsed.addSized(owner) != null) {
            throw new IllegalStateException("Owner already exists in arena");
        }
    }

    protected void removeOwner(RegionAllocatorHandle owner) {
        if (this.ownersByUsed.removeSized(owner) == null) {
            throw new IllegalStateException("Owner does not exist in arena");
        }
    }

    @Override
    void updateUsed(long deltaUsed, RegionAllocatorHandle owner) {
        this.removeOwner(owner);

        var segmentDelta = Long.signum(deltaUsed);
        this.used += deltaUsed;
        owner.used += deltaUsed;
        this.usedSegments += segmentDelta;
        owner.usedSegments += segmentDelta;

        this.addOwner(owner);
    }

    @Override
    public void registerOwner(RegionAllocatorHandle regionAllocatorHandle) {
        super.registerOwner(regionAllocatorHandle);
        this.addOwner(regionAllocatorHandle);
    }

    public float getGlobalFreeFractionAfterEmptying(long totalCapacity, long totalUnfragmentedFree) {
        long otherUnfragmentedFree = totalUnfragmentedFree - this.getBiggestFreeSegmentSize();
        long remainingFreeAfterDealloc = otherUnfragmentedFree - this.used;
        long remainingCapacityAfterDealloc = totalCapacity - this.capacity;
        return (float) remainingFreeAfterDealloc / remainingCapacityAfterDealloc;
    }

    public boolean continueEmptying(ArenaAggregator.DefragBudget budget) {
        if (!this.isEmptying) {
            throw new IllegalStateException("Arena is not emptying");
        }

        int copyCount = 0;
        while (copyCount == 0 && !this.isEmpty()) {
            // get the biggest owner that fits into the budget
            var ownerToEvict = this.ownersByUsed.removeNext();
            if (ownerToEvict == null) {
                throw new IllegalStateException("No owner to evict found even though arena is not empty");
            }

            copyCount = this.estimateAndTransferOwner(ownerToEvict, false);

            // stop emptying if there's no arena that can accommodate the eviction
            if (copyCount == TRANSFER_ABORTED) {
                this.addOwner(ownerToEvict);
                this.isEmptying = false;
                break;
            }

            // notify the owner that has been moved of the buffer change
            ownerToEvict.notifyBufferChanged();

            budget.consumeElementCopy(ownerToEvict.used, copyCount);
        }

        return this.isEmpty();
    }

    private int estimateAndTransferOwner(RegionAllocatorHandle ownerToEvict, boolean allowNewAllocation) {
        return this.estimateAndTransferUploadingOwner(ownerToEvict.usedSegments, ownerToEvict, ownerToEvict.used, allowNewAllocation);
    }

    private int estimateAndTransferUploadingOwner(int finalSegmentCount, RegionAllocatorHandle biggestUsageOwner, long finalUsage, boolean allowNewAllocation) {
        // TODO: when estimating new capacity, take into account how full the section already is since a full section will not grow much anymore
        var newCapacity = BufferArena.estimateNewCapacity(finalSegmentCount, biggestUsageOwner.getFillFractionInv(), finalUsage);
        return this.evictOwner(biggestUsageOwner, newCapacity, allowNewAllocation);
    }

    @Override
    protected void handleResizeUploads(RegionAllocatorHandle uploadingOwner, List<PendingUpload> queue, long totalOwnerUsageAfterUploads) {
        boolean relocatedUploadingOwner = false;

        // this needs to be a loop because the shared buffer isn't guaranteed to be fully defragmented so we may need to evict more than one owner
        do {
            long biggestUsage = Long.MAX_VALUE; // max value to make sure the head map lookup works
            int biggestUsageSegmentCount = 0;
            RegionAllocatorHandle biggestUsageOwner = null;

            if (!relocatedUploadingOwner) {
                biggestUsage = totalOwnerUsageAfterUploads;
                biggestUsageSegmentCount = uploadingOwner.usedSegments + queue.size();
                biggestUsageOwner = uploadingOwner;
            }

            // check if there's another owner that is bigger than this owner will be when it is fully uploaded
            for (var otherOwner : this.ownersByUsed.reversed().values()) {
                if (otherOwner != uploadingOwner && otherOwner.used > biggestUsage) {
                    biggestUsage = otherOwner.used;
                    biggestUsageSegmentCount = otherOwner.usedSegments;
                    biggestUsageOwner = otherOwner;
                    break;
                }
            }

            if (biggestUsageOwner == uploadingOwner) {
                relocatedUploadingOwner = true;
            }

            // by construction, either the owner is the biggest one and is getting moved to its own arena, or another owner is bigger and this one will fit into this young gen arena
            if (biggestUsageOwner == null) {
                throw new IllegalStateException("No owner found to evict");
            }
            this.removeOwner(biggestUsageOwner);
            this.estimateAndTransferUploadingOwner(biggestUsageSegmentCount, biggestUsageOwner, biggestUsage, true);

            // try uploading again
            uploadingOwner.getBackingArena().tryUploads(uploadingOwner, queue);
        } while (!queue.isEmpty());
    }

    private int evictOwner(RegionAllocatorHandle owner, long newCapacity, boolean allowNewAllocation) {
        var targetArena = this.parent.getArenaFittingFor(newCapacity, this.stride, allowNewAllocation);
        if (targetArena == null && !allowNewAllocation) {
            return TRANSFER_ABORTED;
        }
        return this.transferOwnerTo(owner, targetArena);
    }

    private int transferOwnerTo(RegionAllocatorHandle owner, BufferArena targetArena) {
        if (targetArena == this) {
            throw new IllegalStateException("Target arena is the same as the source arena");
        }
        owner.setBackingArena(targetArena);

        // extract all segments owned by this owner, and reassign them to the new arena.
        // we also need to patch up the preceding and following segments to remove references to the extracted segments.
        this.checkAssertions();
        var extractedSegments = this.extractAllSegmentsOwnedBy(owner, targetArena);
        this.checkAssertions();

        // copy the extracted segments into the new arena
        var copyCount = targetArena.receiveSegmentsFrom(extractedSegments, this.arenaBuffer, owner);

        // notify the owner that has been moved of the buffer change
        owner.notifyBufferChanged();

        return copyCount;
    }

    @Override
    protected int receiveSegmentsFrom(List<BufferSegment> segments, GpuBuffer srcBufferObj, RegionAllocatorHandle owner) {
        this.used += owner.used;
        this.usedSegments += segments.size();
        if (this.used > this.capacity) {
            throw new UnsupportedOperationException("New capacity must be larger than used size");
        }

        this.addOwner(owner);

        if (segments.isEmpty()) {
            return 0;
        }

        // find the target segment that's big enough to contain all segments
        var targetSegment = this.takeFree(owner.used);
        if (targetSegment == null) {
            throw new IllegalStateException("No free segment large enough to receive transferred segments even though there should be one");
        }
        long endOfFreePrefix = targetSegment.getEnd() - owner.used;
        var pendingCopies = this.buildTransferList(segments, endOfFreePrefix);

        this.executeCopyCommands(pendingCopies, srcBufferObj, this.arenaBuffer);

        this.finalizeInsertedSegments(targetSegment, endOfFreePrefix, segments);

        return pendingCopies.size();
    }

    private void finalizeInsertedSegments(BufferSegment targetSegment, long endOfFreePrefix, List<BufferSegment> segments) {
        if (segments.isEmpty()) {
            return;
        }

        // new order: targetSegment.prev -> targetSegment (if any space left) -> segments... -> targetSegment.next
        // the target has not yet been resized at this point
        BufferSegment targetPrev = targetSegment.getPrev();
        BufferSegment targetNext = targetSegment.getNext();
        BufferSegment firstInserted = segments.getFirst();
        BufferSegment lastInserted = segments.getLast();

        // link lastInserted and targetNext
        lastInserted.setNext(targetNext);
        if (targetNext != null) {
            targetNext.setPrev(lastInserted);
        }

        // we need to resize the target segment
        if (endOfFreePrefix > targetSegment.getOffset()) {
            // there's space before the inserted segments, resize target to be that free space
            targetSegment.setLength(endOfFreePrefix - targetSegment.getOffset());

            // link targetSegment and firstInserted
            firstInserted.setPrev(targetSegment);
            targetSegment.setNext(firstInserted);
            // prev and target are already linked

            // target segment has already been removed from free list, add it back with new size
            this.addFreeSegment(targetSegment);
        } else {
            // no space before inserted segments, link targetPrev and firstInserted
            firstInserted.setPrev(targetPrev);
            if (targetPrev != null) {
                targetPrev.setNext(firstInserted);
            } else {
                // first inserted is now head
                this.head = firstInserted;
            }
        }

        this.checkAssertions();
    }

    private List<BufferSegment> extractAllSegmentsOwnedBy(RegionAllocatorHandle owner, AllocatorBase newAllocator) {
        ArrayList<BufferSegment> extractedSegments = new ArrayList<>();
        BufferSegment previousExtracted = null;
        BufferSegment current = this.head;
        this.checkAssertions();

        // extract segments owned by the specified owner, patching links of the segments that are not extracted, and correcting the links on the extracted segments to point to each other
        while (current != null) {
            BufferSegment next = current.getNext();

            if (current.getOwner() == owner) {
                // patch links, offsets, and lengths of surrounding segments
                this.extractSegment(current, next);

                extractedSegments.add(current);
                current.setAllocator(newAllocator);
                this.used -= current.getLength();
                this.usedSegments--;

                this.checkAssertions();

                // link extracted segments together
                if (previousExtracted != null) {
                    previousExtracted.setNext(current);
                    current.setPrev(previousExtracted);
                } else {
                    current.setPrev(null);
                }

                previousExtracted = current;
            }
            current = next;
        }

        this.checkAssertions();

        return extractedSegments;
    }

    private void extractSegment(BufferSegment current, BufferSegment next) {
        BufferSegment prev = current.getPrev();

        // current is head
        if (current == this.head) {
            // next is null
            if (next == null) {
                // new free head
                this.head = BufferSegment.createFreeSegment(this, 0, current.getLength());
                this.addFreeSegment(this.head);
            }
            // next is free, expand it
            else if (next.isFree()) {
                this.removeFreeSegment(next);
                this.head = next;
                next.setOffset(0);
                next.setLength(next.getLength() + current.getLength());
                next.setPrev(null);
                this.addFreeSegment(next);
            }
            // next is not free, create new free segment as head
            else {
                this.head = BufferSegment.createFreeSegment(this, 0, current.getLength());
                this.head.setNext(next);
                next.setPrev(this.head);
                this.addFreeSegment(this.head);
            }
        }
        // current is tail
        else if (next == null) {
            // current cannot be head since the other case handles that

            // prev is free, expand it
            if (prev.isFree()) {
                this.removeFreeSegment(prev);
                prev.setLength(prev.getLength() + current.getLength());
                prev.setNext(null);
                this.addFreeSegment(prev);
            }
            // prev is not free, create new free segment as tail
            else {
                BufferSegment newFreeTail = BufferSegment.createFreeSegment(this, current.getOffset(), current.getLength());
                prev.setNext(newFreeTail);
                newFreeTail.setPrev(prev);
                this.addFreeSegment(newFreeTail);
            }
        }
        // current is in the middle
        else {
            // both prev and next are free, expand prev
            if (prev.isFree() && next.isFree()) {
                this.removeFreeSegment(prev);
                this.removeFreeSegment(next);
                prev.setLength(prev.getLength() + current.getLength() + next.getLength());
                prev.setNext(next.getNext());
                if (next.getNext() != null) {
                    next.getNext().setPrev(prev);
                }
                this.addFreeSegment(prev);
            }
            // prev is free, expand it
            else if (prev.isFree()) {
                this.removeFreeSegment(prev);
                prev.setLength(prev.getLength() + current.getLength());
                prev.setNext(next);
                next.setPrev(prev);
                this.addFreeSegment(prev);
            }
            // next is free, expand it
            else if (next.isFree()) {
                this.removeFreeSegment(next);
                next.setOffset(current.getOffset());
                next.setLength(next.getLength() + current.getLength());
                next.setPrev(prev);
                prev.setNext(next);
                this.addFreeSegment(next);
            }
            // neither is free, create new free segment between them
            else {
                BufferSegment newFreeSegment = BufferSegment.createFreeSegment(this, current.getOffset(), current.getLength());
                prev.setNext(newFreeSegment);
                newFreeSegment.setPrev(prev);
                newFreeSegment.setNext(next);
                next.setPrev(newFreeSegment);
                this.addFreeSegment(newFreeSegment);
            }
        }
    }
}
