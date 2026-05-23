package net.caffeinemc.mods.sodium.client.gl.shader.uniform;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlBuffer;
import org.lwjgl.opengl.GL32C;

public class GlUniformBlock {
    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(GpuBuffer buffer) {
        GL32C.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, ((GlBuffer) buffer).handle());
    }
}
