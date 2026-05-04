package net.caffeinemc.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public class DefaultTerrainRenderPasses {
    public static final TerrainRenderPass SOLID = new TerrainRenderPass(ChunkSectionLayer.SOLID, false, false);
    public static final TerrainRenderPass CUTOUT = new TerrainRenderPass(ChunkSectionLayer.CUTOUT, false, true);
    public static final TerrainRenderPass TRANSLUCENT = new TerrainRenderPass(ChunkSectionLayer.TRANSLUCENT, true, true);

    public static final TerrainRenderPass[] ALL = new TerrainRenderPass[] { SOLID, CUTOUT, TRANSLUCENT };

    public static int getPassIndex(TerrainRenderPass pass) {
        if (pass == SOLID) {
            return 0;
        } else if (pass == CUTOUT) {
            return 1;
        } else if (pass == TRANSLUCENT) {
            return 2;
        } else {
            throw new IllegalArgumentException("Unknown terrain render pass");
        }
    }
}
