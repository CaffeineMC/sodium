package net.caffeinemc.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import org.joml.Matrix4fc;

public interface ChunkShaderInterface {
    @Deprecated
    void setupState(TerrainRenderPass pass, GpuSampler sampler, FogParameters parameters);

    @Deprecated
    void resetState();

    void setProjectionMatrix(Matrix4fc matrix);

    void setModelViewMatrix(Matrix4fc matrix);

    void setRegionOffset(float x, float y, float z);

    void setChunkData(GlBuffer data, int time);
}
