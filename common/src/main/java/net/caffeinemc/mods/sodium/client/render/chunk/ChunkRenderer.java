package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    /**
     * Renders the given chunk render list to the active framebuffer.
     *
     * @param matrices                The camera matrices to use for rendering
     * @param renderLists             The collection of render lists
     * @param pass                    The block render pass to execute
     * @param camera                  The camera context containing chunk offsets for the current render
     * @param parameters              The current fog state
     * @param indexedRenderingEnabled Whether indexed rendering is enabled
     * @param terrainSampler          The sampler to use for the atlas
     * @param uniformData             The buffer slice containing the uniform data for this frame
     * @param sectionTimeInfo         The storage buffer containing fade timings
     */
    void render(ChunkRenderMatrices matrices, ChunkRenderListIterable renderLists, TerrainRenderPass pass, CameraTransform camera, FogParameters parameters, boolean indexedRenderingEnabled, GpuSampler terrainSampler, GpuBufferSlice uniformData, GpuBuffer sectionTimeInfo);

    /**
     * Rotates the data for a new frame.
     */
    void rotate();

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();
}
