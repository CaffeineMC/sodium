package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

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
        return renderType.pipeline();
    }

    public RenderTarget getTarget() {
        return renderType.outputTarget();
    }

    public GpuTextureView getAtlas() {
        return renderType.textureView();
    }
}
