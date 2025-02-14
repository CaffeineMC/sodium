package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.minecraft.core.SectionPos;

/**
 * Super class for translucent data that contains an actual buffer.
 */
public abstract class PresentTranslucentData extends TranslucentData {
    private final int quadCount;
    private int quadHash;

    PresentTranslucentData(SectionPos sectionPos, int quadCount) {
        super(sectionPos);
        this.quadCount = quadCount;
    }

    public abstract int[] getVertexCounts();

    public abstract Sorter getSorter();

    @Override
    public boolean oldDataMatches(TranslucentGeometryCollector collector, SortType sortType, TQuad[] quads, int[] vertexCounts) {
        // for the sort types other than NONE (and the old data being AnyOrderData) the geometry needs to be the same (checked with length and hash)
        return this.getQuadCount() == quads.length && this.quadHash == collector.getQuadHash();
    }

    public void setQuadHash(int hash) {
        this.quadHash = hash;
    }

    public int getQuadCount() {
        return this.quadCount;
    }
}
