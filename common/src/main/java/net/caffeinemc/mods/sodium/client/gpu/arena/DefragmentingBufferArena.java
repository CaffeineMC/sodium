package net.caffeinemc.mods.sodium.client.gpu.arena;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.Collection;

public abstract class DefragmentingBufferArena extends BufferArena {
    private static final float DEFRAG_STOP_AFTER_FREE_SEEN_FRACTION = 0.95f;
    private static final float DEFRAG_MIN_FREE_FRACTION = 0.03f;
    private static final int MAX_DEFRAG_STEPS = 5;
    private static final int FRAGMENTATION_DEGREE_SAMPLES = 3;
    private static final int BEST_TARGET_SEARCH_COUNT = 5;

    // profiling has shown that Long2ReferenceRBTreeMap is 58% slower than TreeMap here
    private final SizedTreeMap<BufferSegment> freeSegmentsByLength = new SizedTreeMap<>();

    // direction to move the biggest free segment in during defragmentation
    private boolean defragmentRight = true;

    protected DefragmentingBufferArena(ArenaAggregator parent, GpuBuffer initialBuffer, long capacity, int stride) {
        super(parent, initialBuffer, capacity, stride);
        this.addFreeSegment(this.head);
    }

    protected void addFreeSegment(BufferSegment segment) {
        if (this.freeSegmentsByLength.addSized(segment) != null) {
            CHECK_ASSERTIONS = true;
            this.checkAssertions();
            throw new IllegalStateException("Tried to add a free segment that was already registered as free");
        }
        this.checkSegmentAssertions(segment);
    }

    protected void removeFreeSegment(BufferSegment segment) {
        if (this.freeSegmentsByLength.removeSized(segment) == null) {
            CHECK_ASSERTIONS = true;
            this.checkAssertions();
            throw new IllegalStateException("Tried to remove a free segment that wasn't registered as free");
        }

        // don't check segment assertions on remove because what we're removing is invalid
    }

    public long getBiggestFreeSegmentSize() {
        return this.freeSegmentsByLength.getLargestSize();
    }

    private float calculateFragmentationDegree(Collection<BufferSegment> givenSegments) {
        // take a few of the biggest free segments and sum their sizes
        long biggestFreeTotalSize = 0;
        int count = 0;
        if (givenSegments == null) {
            givenSegments = this.freeSegmentsByLength.descendingMap().values();
        }
        for (var segment : givenSegments) {
            biggestFreeTotalSize += segment.getLength();
            count++;
            if (count >= FRAGMENTATION_DEGREE_SAMPLES) {
                break;
            }
        }
        long totalFreeSize = this.capacity - this.used;
        if (totalFreeSize == 0) {
            return 0.0f;
        }
        return (float) (totalFreeSize - biggestFreeTotalSize) / (float) this.capacity;
    }

    protected void defragmentIncremental(ArenaAggregator.DefragBudget budget) {
        // don't defragment if there's only one free segment
        if (this.freeSegmentsByLength.size() <= 1) {
            return;
        }

        // stop if there's very little free space
        long free = this.capacity - this.used;
        if ((float) free / this.capacity < DEFRAG_MIN_FREE_FRACTION) {
            return;
        }

        var descendingFreeSegments = this.freeSegmentsByLength.descendingMap().values();
        long requiredSeenFreeSize = (long) (free * DEFRAG_STOP_AFTER_FREE_SEEN_FRACTION);

        // calculate number of defragmentation steps to perform based on fragmentation degree
        float fragmentationDegree = this.calculateFragmentationDegree(descendingFreeSegments);
        int defragmentationSteps = calculateDefragmentationSteps(fragmentationDegree);

        for (int i = 0; i < defragmentationSteps; i++) {
            this.defragmentationStep(descendingFreeSegments, requiredSeenFreeSize, budget);
            if (budget.isElementBudgetEmpty()) {
                break;
            }
        }
    }

    private static int calculateDefragmentationSteps(float fragmentationDegree) {
        return Mth.lerpInt(fragmentationDegree, 1, MAX_DEFRAG_STEPS + 1);
    }

    private void defragmentationStep(Collection<BufferSegment> descendingFreeSegments, long requiredSeenFreeSize, ArenaAggregator.DefragBudget budget) {
        // find the biggest free segment that can receive defragmentation
        long seenFreeSize = 0;
        for (BufferSegment segmentToMove : descendingFreeSegments) {
            seenFreeSize += segmentToMove.getLength();

            // stop if we've already seen enough free and thus the degree of fragmentation is low
            if (seenFreeSize >= requiredSeenFreeSize) {
                return;
            }

            if (this.defragmentDirectional(budget, segmentToMove, descendingFreeSegments)) {
                return;
            }
        }

        // no success, go the other way next time
        this.defragmentRight = !this.defragmentRight;
    }

    private boolean defragmentDirectional(ArenaAggregator.DefragBudget budget, BufferSegment biggestFree, Collection<BufferSegment> biggestSegments) {
        // determine the direction we want to move it
        var next = biggestFree.getNext();
        var prev = biggestFree.getPrev();
        if (next == null && prev == this.head) {
            // violated invariant, only one free segment
            throw new IllegalStateException("There cannot be multiple free segments if there's no next and the previous is the head");
        }

        // find as many segments as will fit into the free segment in the chosen direction to move in the opposite direction, which causes the free segment to move in the chosen direction
        var defragmentRightLocal = this.defragmentRight;

        // if there are any, move towards the most worthwhile segments
        var ownOffset = biggestFree.getOffset();
        double bestScore = -1.0;
        var count = 0;
        for (var candidate : biggestSegments) {
            if (candidate == biggestFree) {
                continue;
            }
            if (count++ >= BEST_TARGET_SEARCH_COUNT) {
                break;
            }
            long candidateOffset = candidate.getOffset();
            var candidateIsRight = candidateOffset > ownOffset;
            long distance = candidateIsRight ? candidateOffset - biggestFree.getEnd() : ownOffset - candidate.getEnd();
            
            // Score is the gain (size of the free segment we're moving towards) divided by the cost (distance to move)
            // A higher score means a better target. Add a small value to distance to prevent division by zero.
            double score = (double) candidate.getLength() / (distance + 1.0);
            
            if (score > bestScore) {
                bestScore = score;
                defragmentRightLocal = candidateIsRight;
            }
        }

        if (defragmentRightLocal) {
            if (next != null && this.defragmentRightwards(biggestFree, budget)) {
                this.checkAssertions();
                return true;
            }
        } else {
            if (biggestFree != this.head && this.defragmentLeftwards(biggestFree, budget)) {
                this.checkAssertions();
                return true;
            }
        }
        return false;
    }

    private boolean defragmentRightwards(BufferSegment biggestFree, ArenaAggregator.DefragBudget budget) {
        long freeLength = biggestFree.getLength();
        long freeEnd = biggestFree.getEnd();
        long freeOffset = biggestFree.getOffset();

        long totalMoveLength = 0;
        var toMove = biggestFree.getNext();
        var destinationPrev = biggestFree.getPrev();
        while (toMove != null && !toMove.isFree()) {
            var newTotalMoveLength = totalMoveLength + toMove.getLength();
            if (newTotalMoveLength > freeLength || totalMoveLength != 0 && budget.elementCopyExceedsBudget(newTotalMoveLength)) {
                break;
            }

            // this segment does still fit, add it
            toMove.setOffset(freeOffset + totalMoveLength);
            totalMoveLength = newTotalMoveLength;
            toMove.notifyOwnerSegmentChanged();

            // perform linkages with prev
            if (destinationPrev == null) {
                // moving to head
                this.head = toMove;
            } else {
                destinationPrev.setNext(toMove);
            }
            toMove.setPrev(destinationPrev);

            // get the next segment to check
            destinationPrev = toMove;
            toMove = toMove.getNext();
        }
        // toMove is now the first segment that doesn't fit, or null, or free

        // if there's anything small enough to move
        if (totalMoveLength > 0) {
            // execute the copy of the continuous segments
            long bytes = totalMoveLength * this.stride;
            RenderSystem.getDevice().createCommandEncoder().copyToBuffer(
                    this.arenaBuffer.slice(freeEnd * this.stride, bytes),
                    this.arenaBuffer.slice(freeOffset * this.stride, bytes)
            );
            budget.consumeElementCopy(totalMoveLength, bytes);

            // fix linkages of the last moved segment to the free segment
            destinationPrev.setNext(biggestFree);
            biggestFree.setPrev(destinationPrev);

            // adjust the free segment
            this.removeFreeSegment(biggestFree);
            biggestFree.setOffset(freeOffset + totalMoveLength);

            // check for merging with next free segment
            if (toMove != null && toMove.isFree()) {
                this.removeFreeSegment(toMove);
                biggestFree.setLength(biggestFree.getLength() + toMove.getLength());
                biggestFree.setNext(toMove.getNext());
                if (toMove.getNext() != null) {
                    toMove.getNext().setPrev(biggestFree);
                }
            } else {
                biggestFree.setNext(toMove);
                if (toMove != null) {
                    toMove.setPrev(biggestFree);
                }
            }

            this.addFreeSegment(biggestFree);

            return true;
        }

        return false;
    }

    // note that there is no this.tail
    private boolean defragmentLeftwards(BufferSegment biggestFree, ArenaAggregator.DefragBudget budget) {
        long freeLength = biggestFree.getLength();
        long freeEnd = biggestFree.getEnd();
        long freeOffset = biggestFree.getOffset();

        long totalMoveLength = 0;
        var toMove = biggestFree.getPrev();
        var destinationNext = biggestFree.getNext();
        while (toMove != null && !toMove.isFree()) {
            var newTotalMoveLength = totalMoveLength + toMove.getLength();
            if (newTotalMoveLength > freeLength || totalMoveLength != 0 && budget.elementCopyExceedsBudget(newTotalMoveLength)) {
                break;
            }

            // this segment does still fit, add it
            totalMoveLength = newTotalMoveLength;
            toMove.setOffset(freeEnd - totalMoveLength);
            toMove.notifyOwnerSegmentChanged();

            // perform linkages with next
            if (destinationNext != null) {
                destinationNext.setPrev(toMove);
            }
            toMove.setNext(destinationNext);

            // get the next segment to check
            destinationNext = toMove;
            toMove = toMove.getPrev();
        }
        // toMove is now the first segment that doesn't fit, or null, or free

        // if there's anything small enough to move
        if (totalMoveLength > 0) {
            // execute the copy of the continuous segments
            long bytes = totalMoveLength * this.stride;
            RenderSystem.getDevice().createCommandEncoder().copyToBuffer(
                    this.arenaBuffer.slice((freeOffset - totalMoveLength) * this.stride, bytes),
                    this.arenaBuffer.slice((freeEnd - totalMoveLength) * this.stride, bytes)
            );
            budget.consumeElementCopy(totalMoveLength, bytes);

            // fix linkages of the last moved segment to the free segment
            destinationNext.setPrev(biggestFree);
            biggestFree.setNext(destinationNext);

            // adjust the free segment
            this.removeFreeSegment(biggestFree);

            // run with asserts enabled in mangrove forest: the overlapping segments are probably the cause
            if (freeOffset < totalMoveLength) {
                CHECK_ASSERTIONS = true;
                this.checkAssertions();
                throw new IllegalStateException("Invalid segments resulted in negative offset during defragmentation");
            }
            biggestFree.setOffset(freeOffset - totalMoveLength);

            // check for merging with prev free segment
            if (toMove != null && toMove.isFree()) {
                this.removeFreeSegment(toMove);
                biggestFree.setOffset(toMove.getOffset());
                biggestFree.setLength(freeLength + toMove.getLength());
                biggestFree.setPrev(toMove.getPrev());
                if (toMove.getPrev() != null) {
                    toMove.getPrev().setNext(biggestFree);
                } else {
                    this.head = biggestFree;
                }
            } else {
                biggestFree.setPrev(toMove);
                if (toMove != null) {
                    toMove.setNext(biggestFree);
                } else {
                    this.head = biggestFree;
                }
            }

            this.addFreeSegment(biggestFree);

            return true;
        }

        return false;
    }

    @Override
    BufferSegment takeFree(long size) {
        return this.freeSegmentsByLength.removeFirstOfSizeAtLeast(size);
    }

    @Override
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
            if (free.getLength() < size) {
                CHECK_ASSERTIONS = true;
                this.checkAssertions();
                throw new IllegalStateException("Free segment is smaller than requested size");
            }
            result = new BufferSegment(this, owner, ownerIndex, free.getEnd() - size, size);
            result.setNext(free.getNext());
            result.setPrev(free);

            if (result.getNext() != null) {
                result.getNext().setPrev(result);
            }

            free.setLength(free.getLength() - size);
            free.setNext(result);

            this.addFreeSegment(free);
        }

        this.updateUsed(result.getLength(), owner);
        this.checkAssertions();

        return result;
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
        boolean nextFree = next != null && next.isFree();
        BufferSegment prev = entry.getPrev();
        boolean prevFree = prev != null && prev.isFree();

        // both free, merge with both
        if (nextFree && prevFree) {
            this.removeFreeSegment(prev);
            this.removeFreeSegment(next);
            prev.setLength(prev.getLength() + entry.getLength() + next.getLength());
            prev.setNext(next.getNext());
            if (next.getNext() != null) {
                next.getNext().setPrev(prev);
            }
            this.addFreeSegment(prev);
        } else if (nextFree) {
            this.removeFreeSegment(next);
            entry.setLength(entry.getLength() + next.getLength());
            entry.setNext(next.getNext());
            if (next.getNext() != null) {
                next.getNext().setPrev(entry);
            }
            this.addFreeSegment(entry);
        } else if (prevFree) {
            this.removeFreeSegment(prev);
            prev.setLength(prev.getLength() + entry.getLength());
            prev.setNext(entry.getNext());
            if (entry.getNext() != null) {
                entry.getNext().setPrev(prev);
            }
            this.addFreeSegment(prev);
        } else {
            this.addFreeSegment(entry);
        }

        this.checkAssertions();
    }

    @Override
    public void renderDebugMap(GuiGraphicsExtractor graphics, int x, int y, int drawWidth, int drawHeight) {
        super.renderDebugMap(graphics, x, y, drawWidth, drawHeight);

        // render measure of fragmentation degree and copies performed per frame
        float fragmentationDegree = this.calculateFragmentationDegree(null);
        int defragmentationSteps = calculateDefragmentationSteps(fragmentationDegree);
        int barLength = (int) (drawHeight * fragmentationDegree);
        var thickness = 3;
        graphics.fill(x, y, x + thickness, y + barLength, 0xCFFFFFFF);
        graphics.text(Minecraft.getInstance().font, Integer.toString(defragmentationSteps), x, y, 0xFFFFFFFF);
    }
}
