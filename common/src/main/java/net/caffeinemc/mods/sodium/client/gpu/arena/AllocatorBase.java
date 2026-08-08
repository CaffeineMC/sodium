package net.caffeinemc.mods.sodium.client.gpu.arena;

import com.mojang.renderpearl.api.buffers.GpuBuffer;

public interface AllocatorBase {
    long getDeviceUsedMemory();

    long getDeviceAllocatedMemory();

    void free(BufferSegment entry);

    boolean isEmpty();

    GpuBuffer getBufferObject();
}
