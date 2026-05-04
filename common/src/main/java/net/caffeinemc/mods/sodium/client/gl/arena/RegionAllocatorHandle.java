package net.caffeinemc.mods.sodium.client.gl.arena;

import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
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
        void onBufferChanged(CommandList commandList);

        void onSegmentChanged(CommandList commandList, int ownerIndex);
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

    public void deleteSingleOwner(CommandList commands) {
        // differentiation of single-owner or shared deletion is handled at the arena level
        this.backingArena.deleteSingleOwner(commands, this);
    }

    @Override
    public boolean isEmpty() {
        return this.backingArena.isEmpty();
    }

    @Override
    public GlBuffer getBufferObject() {
        return this.backingArena.getBufferObject();
    }

    public boolean upload(CommandList commandList, Stream<PendingUpload> stream) {
        var prevBackingArena = this.backingArena;
        var bufferChanged = this.backingArena.upload(commandList, this, stream);
        return bufferChanged || this.backingArena != prevBackingArena;
    }

    public BufferArena getBackingArena() {
        return this.backingArena;
    }

    public boolean isSingleOwner() {
        return !(this.backingArena instanceof SharedBufferArena);
    }

    public void notifyBufferChanged(CommandList commandList) {
        this.onChange.onBufferChanged(commandList);
    }

    public void notifySegmentChanged(CommandList commandList, int ownerIndex) {
        this.onChange.onSegmentChanged(commandList, ownerIndex);
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
