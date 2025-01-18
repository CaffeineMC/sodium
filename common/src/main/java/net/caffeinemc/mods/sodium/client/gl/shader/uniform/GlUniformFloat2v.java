package net.caffeinemc.mods.sodium.client.gl.shader.uniform;

import org.lwjgl.opengl.GL30C;

public class GlUniformFloat2v extends GlUniform<float[]> {
    public GlUniformFloat2v(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        if (value.length != 2) {
            throw new IllegalArgumentException("value.length != 2");
        }

        GL30C.glUniform2fv(this.index, value);
    }

    public void set(float x, float y) {
        GL30C.glUniform2f(this.index, x, y);
    }
}
