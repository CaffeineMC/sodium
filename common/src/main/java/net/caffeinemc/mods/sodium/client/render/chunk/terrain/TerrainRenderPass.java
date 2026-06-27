package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlas;

public class TerrainRenderPass {
    @Deprecated(forRemoval = true)
    private final ChunkSectionLayer renderType;

    private final boolean isTranslucent;
    private final boolean fragmentDiscard;

    public TerrainRenderPass(ChunkSectionLayer renderType, boolean isTranslucent, boolean allowFragmentDiscard) {
        this.renderType = renderType;

        this.isTranslucent = isTranslucent;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    public boolean isTranslucent() {
        return this.isTranslucent;
    }

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }

    public RenderPipeline getPipeline() {
        return this.renderType.pipeline();
    }

    public RenderTarget getTarget() {
        return (this.isTranslucent && Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()) ? Minecraft.getInstance().levelRenderer.translucentTarget() : Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    public GpuTextureView getAtlas() {
        return Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
    }
}
