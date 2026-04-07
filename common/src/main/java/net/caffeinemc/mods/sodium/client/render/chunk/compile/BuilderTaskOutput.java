package net.caffeinemc.mods.sodium.client.render.chunk.compile;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.MeshResultSize;

public abstract class BuilderTaskOutput {
    public final RenderSection section;
    public final int submitTime;
    private long resultSize = MeshResultSize.NO_DATA;

    public BuilderTaskOutput(RenderSection section, int buildTime) {
        this.section = section;
        this.submitTime = buildTime;
    }

    public void destroy() {
    }

    protected abstract long calculateResultSize();

    public long getResultSize() {
        if (this.resultSize == MeshResultSize.NO_DATA) {
            this.resultSize = this.calculateResultSize();
        }
        return this.resultSize;
    }
}
