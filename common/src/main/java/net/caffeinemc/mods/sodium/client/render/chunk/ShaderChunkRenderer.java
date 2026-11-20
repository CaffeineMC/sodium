package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderCompilationParameters;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.SodiumTerrainPipelines;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.oit.OitStage;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.jspecify.annotations.Nullable;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    protected final ChunkVertexType vertexType;
    protected final VertexFormat vertexFormat;

    protected RenderPipeline activeProgram;

    public ShaderChunkRenderer(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getVertexFormat();
    }

    protected void begin(TerrainRenderPass pass, @Nullable OitStage stage, boolean hasWaterMask) {
        ShaderCompilationParameters parameters = new ShaderCompilationParameters(pass, stage, Minecraft.getInstance().options.improvedFog().get(), Minecraft.getInstance().gameRenderer.useImprovedTransparency(), hasWaterMask, SodiumClientMod.options().performance.useSphericalRenderFog, this.vertexType);
        this.activeProgram = SodiumTerrainPipelines.compileProgram(parameters);
    }

    protected void end() {
        this.activeProgram = null;
    }

    @Override
    public void delete() {
    }

}
