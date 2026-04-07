package net.caffeinemc.mods.sodium.client.render.chunk.compile;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.Sorter;

public class ChunkSortOutput extends BuilderTaskOutput {
    private Sorter sorter;
    private boolean containsNewIndexData;

    public ChunkSortOutput(RenderSection render, int buildTime) {
        super(render, buildTime);
    }

    public ChunkSortOutput(RenderSection render, int buildTime, Sorter data) {
        this(render, buildTime);
        this.setSorter(data);
    }

    public void setSorter(Sorter sorter) {
        this.sorter = sorter;
        this.containsNewIndexData = true;
    }

    public Sorter getSorter() {
        return this.sorter;
    }

    public void markAsNotContainingNewIndexData() {
        this.containsNewIndexData = false;
    }

    public boolean containsNewIndexData() {
        return this.containsNewIndexData;
    }

    public void destroy() {
        super.destroy();
        if (this.sorter != null) {
            this.sorter.destroy();
        }
    }

    @Override
    protected long calculateResultSize() {
        if (this.sorter == null) {
            return 0;
        }
        var indexBuffer = this.sorter.getIndexBuffer();
        if (indexBuffer == null) {
            return 0;
        }
        return indexBuffer.getLength();
    }
}
