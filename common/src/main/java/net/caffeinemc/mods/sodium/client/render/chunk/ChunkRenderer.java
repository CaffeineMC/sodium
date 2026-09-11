package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderListIterable;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.minecraft.client.renderer.oit.OitStage;
import org.jspecify.annotations.Nullable;

/**
 * The chunk render backend takes care of managing the graphics resource state of chunk render containers. This includes
 * the handling of uploading their data to the graphics card and rendering responsibilities.
 */
public interface ChunkRenderer {
    /**
     * Prepares this frame's chunk lists for rendering.
     */
    void prepare(ChunkRenderListIterable renderLists, CameraTransform camera, boolean indexedRenderingEnabled);

    /**
     * Renders the given chunk render list to the active framebuffer.
     *
     * @param matrices                The camera matrices to use for rendering
     * @param renderLists             The collection of render lists
     * @param camera                  The camera context containing chunk offsets for the current render
     * @param parameters              The current fog state
     * @param indexedRenderingEnabled Whether indexed rendering is enabled
     * @param pass                    The block render pass to execute
     * @param terrainSampler          The sampler to use for the atlas
     * @param uniformData             The buffer slice containing the uniform data for this frame
     * @param sectionTimeInfo         The storage buffer containing fade timings
     * @param stage                   The OIT stage to render to, or null if it's not relevant
     */
    void render(ChunkRenderMatrices matrices, ChunkRenderListIterable renderLists, TerrainRenderPass terrainRenderPass, CameraTransform camera, FogParameters parameters, boolean indexedRenderingEnabled, RenderPass pass, GpuSampler terrainSampler, GpuBufferSlice uniformData, GpuBuffer sectionTimeInfo, @Nullable OitStage stage);

    /**
     * Rotates the data for a new frame.
     */
    void rotate();

    /**
     * Deletes this render backend and any resources attached to it.
     */
    void delete();
}
