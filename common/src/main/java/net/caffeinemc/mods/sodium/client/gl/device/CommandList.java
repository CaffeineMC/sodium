package net.caffeinemc.mods.sodium.client.gl.device;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;

import java.nio.ByteBuffer;

public interface CommandList extends AutoCloseable {


    void flush();

    @Override
    default void close() {
        this.flush();
    }

    void flushMappedRange(GpuBufferSlice.MappedView map, int start, int i);
}
