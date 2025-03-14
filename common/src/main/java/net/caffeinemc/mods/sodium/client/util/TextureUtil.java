package net.caffeinemc.mods.sodium.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;

public class TextureUtil {

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static GpuTexture getLightTextureId() {
        return RenderSystem.getShaderTexture(2);
    }

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static GpuTexture getBlockTextureId() {
        return RenderSystem.getShaderTexture(0);
    }
}
