package net.caffeinemc.mods.sodium.client.util;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import org.jspecify.annotations.Nullable;

public class SodiumChunkSection extends ChunkSectionsToRender {
    private final SodiumWorldRenderer renderer;
    private final ChunkRenderMatrices matrices;
    private final double x, y, z;

    public SodiumChunkSection(SodiumWorldRenderer renderer, ChunkRenderMatrices matrices, double x, double y, double z) {
        super(null, 0);

        this.renderer = renderer;
        this.matrices = matrices;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    protected void render(ChunkSectionLayer layer, RenderPass renderPass, @Nullable GpuBuffer defaultIndexBuffer, @Nullable IndexType defaultIndexType, @Nullable RenderPipeline renderPipelineOverride, @Nullable RenderPipeline renderPipelineOverrideMultidraw) {
        throw new IllegalStateException("Not possible?");
    }

    @Override
    public void renderGroup(ChunkSectionLayerGroup group, RenderPass renderPass, GpuSampler sampler, GpuTextureView atlas, boolean renderWireframeTerrain) {
        this.renderer.drawChunkLayer(renderPass, group, this.matrices, this.x, this.y, this.z, sampler, null);
    }

    @Override
    public void renderOit(GpuSampler sampler, OitStage stage, OitRenderPassProvider.Parameters params, GpuTextureView atlas, GpuTextureView lightmap) {
        try (RenderPass renderPass = OitRenderPassProvider.createRenderPass(stage, () -> "Terrain (Sodium)", params)) {
            this.renderer.drawChunkLayer(renderPass, ChunkSectionLayerGroup.TRANSLUCENT, this.matrices, this.x, this.y, this.z, sampler, stage);
        }
    }
}
