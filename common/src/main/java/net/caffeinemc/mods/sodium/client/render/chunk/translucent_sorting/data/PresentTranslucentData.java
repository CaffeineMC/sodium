package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.minecraft.core.SectionPos;

/**
 * Super class for translucent data that contains an actual buffer.
 */
public abstract class PresentTranslucentData extends TranslucentData {
    private final int inputQuadCount;
    private int quadHash;

    PresentTranslucentData(SectionPos sectionPos, int inputQuadCount) {
        super(sectionPos);
        this.inputQuadCount = inputQuadCount;
    }

    public abstract Sorter getSorter();

    @Override
    public boolean oldDataMatches(TranslucentGeometryCollector collector, SortType sortType, TQuad[] quads) {
        // for the sort types other than NONE (and the old data being AnyOrderData) the geometry needs to be the same (checked with length and hash)
        return this.getInputQuadCount() == quads.length && this.hashMatches(collector);
    }

    protected boolean hashMatches(TranslucentGeometryCollector collector) {
        return this.quadHash == collector.getQuadHash();
    }

    public void setQuadHash(int hash) {
        this.quadHash = hash;
    }

    public int getInputQuadCount() {
        return this.inputQuadCount;
    }

    public int getIndexQuadCount() {
        return this.inputQuadCount;
    }
}
