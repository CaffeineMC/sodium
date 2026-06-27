package net.caffeinemc.mods.sodium.client.gpu.arena;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;

import java.util.stream.Stream;

public class RegionAllocatorHandle implements AllocatorBase, SizedTreeMap.Sized {
    private final RenderRegion region;
    private final AllocationChangeConsumer onChange;
    private BufferArena backingArena;
    long used;
    int usedSegments;
    int identifier;
    private static int nextIdentifier = 1;

    public RegionAllocatorHandle(RenderRegion region, AllocationChangeConsumer onChange, BufferArena backingArena) {
        this.region = region;
        this.onChange = onChange;
        this.backingArena = backingArena;
        this.identifier = nextIdentifier++;

        this.backingArena.registerOwner(this);
    }

    public interface AllocationChangeConsumer {
        void onBufferChanged();

        void onSegmentChanged(int ownerIndex);
    }

    float getFillFractionInv() {
        return this.region.getFillFractionInv();
    }

    void setBackingArena(BufferArena arena) {
        this.backingArena = arena;
    }

    @Override
    public long getDeviceAllocatedMemory() {
        return this.backingArena.getDeviceAllocatedMemory();
    }

    @Override
    public long getDeviceUsedMemory() {
        return this.backingArena.getDeviceUsedMemory();
    }

    @Override
    public void free(BufferSegment entry) {
        this.backingArena.free(entry);
    }

    public void deleteSingleOwner() {
        // differentiation of single-owner or shared deletion is handled at the arena level
        this.backingArena.deleteSingleOwner(this);
    }

    @Override
    public boolean isEmpty() {
        return this.backingArena.isEmpty();
    }

    @Override
    public GpuBuffer getBufferObject() {
        return this.backingArena.getBufferObject();
    }

    public boolean upload(Stream<PendingUpload> stream) {
        var prevBackingArena = this.backingArena;
        var bufferChanged = this.backingArena.upload(this, stream);
        return bufferChanged || this.backingArena != prevBackingArena;
    }

    public BufferArena getBackingArena() {
        return this.backingArena;
    }

    public boolean isSingleOwner() {
        return !(this.backingArena instanceof SharedBufferArena);
    }

    public void notifyBufferChanged() {
        this.onChange.onBufferChanged();
    }

    public void notifySegmentChanged(int ownerIndex) {
        this.onChange.onSegmentChanged(ownerIndex);
    }

    @Override
    public long getSize() {
        return this.used;
    }

    @Override
    public long getIdentifier() {
        return this.identifier;
    }
}
