package net.caffeinemc.mods.sodium.client.gpu.arena.staging;

import com.mojang.blaze3d.buffers.GpuBuffer;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(ByteBuffer data, GpuBuffer dst, long writeOffset);

    void flush();

    void delete();

    void flip();

    long getUploadSizeLimit(long frameDuration);
}
