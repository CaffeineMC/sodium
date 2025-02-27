package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.FullTQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;

import java.nio.ByteBuffer;

public class UpdatedQuadsList extends ReferenceArrayList<FullTQuad> {
    private int meshQuadCount;
    private int indexQuadCount;

    public int getMeshQuadCount() {
        return this.meshQuadCount;
    }

    public int getIndexQuadCount() {
        return this.indexQuadCount;
    }

    public void setQuadCounts(int meshQuadCount, int indexQuadCount) {
        this.meshQuadCount = meshQuadCount;
        this.indexQuadCount = indexQuadCount;
    }

    public void applyBufferUpdates(ChunkMeshBufferBuilder builder, ByteBuffer buffer) {
        for (var quad : this) {
            quad.writeToBuffer(builder, buffer);
        }
    }
}
