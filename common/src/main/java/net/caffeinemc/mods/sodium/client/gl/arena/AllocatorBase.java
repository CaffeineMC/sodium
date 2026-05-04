package net.caffeinemc.mods.sodium.client.gl.arena;

import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;

public interface AllocatorBase {
    long getDeviceUsedMemory();

    long getDeviceAllocatedMemory();

    void free(BufferSegment entry);

    boolean isEmpty();

    GlBuffer getBufferObject();
}
