package net.caffeinemc.mods.sodium.client.world;

import com.mojang.renderpearl.api.textures.GpuSampler;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import org.jspecify.annotations.Nullable;

public interface LevelRendererExtension {
    SodiumWorldRenderer sodium$getWorldRenderer();

    /**
     * Hook for mods to change the matrices.
     * @param matrices The new chunk matrices.
     */
    void sodium$setMatrices(ChunkRenderMatrices matrices);

    ChunkRenderMatrices sodium$getMatrices();

    GpuSampler sodium$getTerrainSampler();
}
