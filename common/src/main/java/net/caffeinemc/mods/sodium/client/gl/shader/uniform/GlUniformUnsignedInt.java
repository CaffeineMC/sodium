package net.caffeinemc.mods.sodium.client.gl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class GlUniformUnsignedInt extends GlUniform<Integer> {
    public GlUniformUnsignedInt(int index) {
        super(index);
    }

    @Override
    public void set(Integer value) {
        this.setInt(value);
    }

    public void setInt(int value) {
        GL30C.glUniform1ui(this.index, value);
    }
}
