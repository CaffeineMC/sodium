package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.client.renderer.oit.OitStage;

public record ShaderCompilationParameters(TerrainRenderPass pass, OitStage stage, boolean improvedFog, boolean cloudsInFog, boolean useWaterMask, boolean useSphericalFog, ChunkVertexType vertexType) {
}
