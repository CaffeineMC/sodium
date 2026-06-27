package net.caffeinemc.mods.sodium.client.gpu.arena;

import com.mojang.blaze3d.buffers.GpuBuffer;

public interface AllocatorBase {
    long getDeviceUsedMemory();

    long getDeviceAllocatedMemory();

    void free(GlBufferSegment entry);

    boolean isEmpty();

    GpuBuffer getBufferObject();
}
