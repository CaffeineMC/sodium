package net.caffeinemc.mods.sodium.client.gl.buffer;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.caffeinemc.mods.sodium.client.gl.GlObject;
import org.lwjgl.opengl.GL46C;

public class GlTexelBuffer extends GlObject {
    public GlTexelBuffer(int buffer, int format) {
        super();

        this.setHandle(GlStateManager._genTexture());
        GL46C.glBindTexture(GL46C.GL_TEXTURE_BUFFER, this.handle());
        GL46C.glTexBuffer(GL46C.GL_TEXTURE_BUFFER, format, buffer);
    }

    public void destroy() {
        GlStateManager._deleteTexture(this.handle());
        this.invalidateHandle();
    }
}
