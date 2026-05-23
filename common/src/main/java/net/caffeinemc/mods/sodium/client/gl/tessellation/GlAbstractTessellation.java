package net.caffeinemc.mods.sodium.client.gl.tessellation;

import com.mojang.blaze3d.opengl.GlBuffer;
import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;

public abstract class GlAbstractTessellation implements GlTessellation {
    protected final GlPrimitiveType primitiveType;
    protected final TessellationBinding[] bindings;

    protected GlAbstractTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings) {
        this.primitiveType = primitiveType;
        this.bindings = bindings;
    }

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    protected void bindAttributes(CommandList commandList) {
        for (TessellationBinding binding : this.bindings) {
            GL33C.glBindBuffer(binding.target().getTargetParameter(), ((GlBuffer) binding.buffer()).handle());

            for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
                if (attrib.isIntType()) {
                    GL30C.glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(),
                            attrib.getStride(), attrib.getPointer());
                } else {
                    GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(), attrib.isNormalized(),
                            attrib.getStride(), attrib.getPointer());
                }
                GL20C.glEnableVertexAttribArray(attrib.getIndex());
            }
        }
    }
}
