package net.caffeinemc.mods.sodium.client.gl.shader.uniform;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;
import net.caffeinemc.mods.sodium.mixin.core.GlBufferAccessor;
import org.lwjgl.opengl.GL32C;

public class GlUniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(GlBuffer buffer) {
        GL32C.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, buffer.handle());
    }

    public void bindBufferRange(GpuBufferSlice slice) {
        GL32C.glBindBufferRange(GL32C.GL_UNIFORM_BUFFER, this.binding, ((GlBufferAccessor) slice.buffer()).sodium$getHandle(), slice.offset(), slice.length());
    }
}
