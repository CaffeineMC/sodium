package net.caffeinemc.mods.sodium.client.gpu.arena;

import net.caffeinemc.mods.sodium.client.util.UInt32;

// TODO: fine-grained segment update notification to avoid re-writing the entire render data on small changes
public class BufferSegment implements SizedTreeMap.Sized {
    private AllocatorBase allocator;
    private RegionAllocatorHandle owner;
    private int ownerIndex;

    private int offset; /* Uint32 */
    private int length; /* Uint32 */

    private BufferSegment next;
    private BufferSegment prev;

    public BufferSegment(BufferArena allocator, RegionAllocatorHandle owner, int ownerIndex, long offset, long length) {
        this.allocator = allocator;
        this.owner = owner;
        this.ownerIndex = ownerIndex;
        this.offset = UInt32.downcast(offset);
        this.length = UInt32.downcast(length);
    }

    public static BufferSegment createFreeSegment(BufferArena allocator, long offset, long length) {
        return new BufferSegment(allocator, null, 0, offset, length);
    }

    /* Uint32 */
    protected long getEnd() {
        return this.getOffset() + this.getLength();
    }

    /* Uint32 */
    public long getOffset() {
        return UInt32.upcast(this.offset);
    }

    /* Uint32 */
    public long getLength() {
        return UInt32.upcast(this.length);
    }

    protected void setOffset(long offset /* Uint32 */) {
        this.offset = UInt32.downcast(offset);
    }

    protected void setLength(long length /* Uint32 */) {
        this.length = UInt32.downcast(length);
    }

    protected void setOwner(RegionAllocatorHandle owner, int ownerIndex) {
        this.owner = owner;
        this.ownerIndex = ownerIndex;
    }

    protected void notifyOwnerSegmentChanged() {
        this.owner.notifySegmentChanged(this.ownerIndex);
    }

    protected void setFree() {
        this.owner = null;
    }

    protected boolean isFree() {
        return this.owner == null;
    }

    protected void setNext(BufferSegment next) {
        this.next = next;
    }

    protected BufferSegment getNext() {
        return this.next;
    }

    protected BufferSegment getPrev() {
        return this.prev;
    }

    protected void setPrev(BufferSegment prev) {
        this.prev = prev;
    }

    public void delete() {
        this.allocator.free(this);
    }

    void setAllocator(AllocatorBase allocator) {
        this.allocator = allocator;
    }

    protected RegionAllocatorHandle getOwner() {
        return this.owner;
    }

    protected void mergeInto(BufferSegment entry) {
        this.setLength(this.getLength() + entry.getLength());
        this.setNext(entry.getNext());

        if (this.getNext() != null) {
            this.getNext().setPrev(this);
        }
    }

    @Override
    public long getSize() {
        return this.getLength();
    }

    @Override
    public long getIdentifier() {
        return this.getOffset();
    }
}
