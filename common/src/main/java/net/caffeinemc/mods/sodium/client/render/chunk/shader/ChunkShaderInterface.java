package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlTexelBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;

public interface ChunkShaderInterface {
    @Deprecated
    void setupState(TerrainRenderPass pass, FogParameters parameters, GpuSampler terrainSampler, GpuBufferSlice uniformData, GlTexelBuffer sectionTimeInfo);

    @Deprecated
    void resetState();

    void setRegionData(CameraTransform camera, RenderRegion region);
}
