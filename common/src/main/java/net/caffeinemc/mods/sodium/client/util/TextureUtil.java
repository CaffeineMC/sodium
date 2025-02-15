package net.caffeinemc.mods.sodium.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

public class TextureUtil {

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static GpuTexture getLightTexture() {
        return RenderSystem.getShaderTexture(2);
    }

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static GpuTexture getBlockTexture() {
        return RenderSystem.getShaderTexture(0);
    }
}
