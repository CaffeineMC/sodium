package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class DefaultTerrainRenderPasses {
    public static final TerrainRenderPass SOLID = new TerrainRenderPass(ChunkSectionLayer.SOLID, false, false);
    public static final TerrainRenderPass CUTOUT = new TerrainRenderPass(ChunkSectionLayer.CUTOUT, false, true);
    public static final TerrainRenderPass TRANSLUCENT = new TerrainRenderPass(ChunkSectionLayer.TRANSLUCENT, true, true);


    public static final TerrainRenderPass[] ALL = new TerrainRenderPass[] { SOLID, CUTOUT, TRANSLUCENT };
}
