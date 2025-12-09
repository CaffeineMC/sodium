package net.caffeinemc.mods.sodium.client.gl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class GlUniformBool extends GlUniform<Boolean> {
    public GlUniformBool(int index) {
        super(index);
    }

    @Override
    public void set(Boolean value) {
        this.setBool(value);
    }

    public void setBool(boolean value) {
        GL30C.glUniform1i(this.index, value ? 1 : 0);
    }
}
