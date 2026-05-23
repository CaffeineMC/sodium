package net.caffeinemc.mods.sodium.client.gl.device;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.gl.array.GlVertexArray;
import net.caffeinemc.mods.sodium.client.gl.buffer.*;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlTessellation;
import net.caffeinemc.mods.sodium.client.gl.tessellation.TessellationBinding;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;

import java.nio.ByteBuffer;

public interface CommandList extends AutoCloseable {

    GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings);

    void bindVertexArray(GlVertexArray array);
    void unbindVertexArray();

    void deleteVertexArray(GlVertexArray vertexArray);

    void flush();

    DrawCommandList beginTessellating(GlTessellation tessellation);

    void deleteTessellation(GlTessellation tessellation);

    @Override
    default void close() {
        this.flush();
    }

    void flushMappedRange(GpuBufferSlice.MappedView map, int start, int i);
}
