package net.caffeinemc.mods.sodium.client.gl.arena.staging;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(CommandList commandList, ByteBuffer data, GpuBuffer dst, long writeOffset);

    void flush(CommandList commandList);

    void delete(CommandList commandList);

    void flip();

    long getUploadSizeLimit(long frameDuration);
}
