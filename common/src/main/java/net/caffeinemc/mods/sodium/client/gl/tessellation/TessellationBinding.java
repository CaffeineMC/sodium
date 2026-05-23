package net.caffeinemc.mods.sodium.client.gl.tessellation;

import com.mojang.blaze3d.buffers.GpuBuffer;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferTarget;

public record TessellationBinding(GlBufferTarget target,
                                  GpuBuffer buffer,
                                  GlVertexAttributeBinding[] attributeBindings) {
    public static TessellationBinding forVertexBuffer(GpuBuffer buffer, GlVertexAttributeBinding[] attributes) {
        return new TessellationBinding(GlBufferTarget.ARRAY_BUFFER, buffer, attributes);
    }

    public static TessellationBinding forElementBuffer(GpuBuffer buffer) {
        return new TessellationBinding(GlBufferTarget.ELEMENT_BUFFER, buffer, new GlVertexAttributeBinding[0]);
    }
}
