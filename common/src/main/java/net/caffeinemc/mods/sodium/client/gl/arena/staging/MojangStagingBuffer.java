package net.caffeinemc.mods.sodium.client.gl.arena.staging;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;

import java.nio.ByteBuffer;

public class MojangStagingBuffer implements StagingBuffer {
    private final MappedStagingBuffer staging;

    public MojangStagingBuffer(int size) {
        if (RenderSystem.getDevice().getDeviceInfo().features().persistentMapping()) {
            this.staging = new MappedStagingBuffer(size);
        } else {
            this.staging = null;
        }
    }

    @Override
    public void enqueueCopy(CommandList commandList, ByteBuffer data, GpuBuffer dst, long writeOffset) {
        if (this.staging == null) {
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(dst.slice(writeOffset, data.remaining()), data);
        } else {
            staging.enqueueCopy(commandList, data, dst, writeOffset);
        }
    }

    @Override
    public void flush(CommandList commandList) {
        if (staging != null) staging.flush(commandList);
    }

    @Override
    public void delete(CommandList commandList) {
        if (staging != null) staging.delete(commandList);
    }

    @Override
    public void flip() {
        if (staging != null) staging.flip();
    }

    @Override
    public long getUploadSizeLimit(long frameDuration) {
        return staging != null ? staging.getUploadSizeLimit(frameDuration) : 25600000;
    }

    @Override
    public String toString() {
        return staging != null ? staging.toString() : "Fallback";
    }
}
