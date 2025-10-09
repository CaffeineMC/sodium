package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class DefaultTerrainRenderPasses {
    public static final TerrainRenderPass SOLID = new TerrainRenderPass(ChunkSectionLayer.SOLID, false);
    public static final TerrainRenderPass CUTOUT = new TerrainRenderPass(ChunkSectionLayer.CUTOUT, true);
    public static final TerrainRenderPass TRANSLUCENT = new TerrainRenderPass(ChunkSectionLayer.TRANSLUCENT, false);


    public static final TerrainRenderPass[] ALL = new TerrainRenderPass[] { SOLID, CUTOUT, TRANSLUCENT };
}
