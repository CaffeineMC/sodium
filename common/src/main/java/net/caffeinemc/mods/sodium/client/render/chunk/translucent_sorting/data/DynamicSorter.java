package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

public abstract class DynamicSorter extends PresentSorter {
    private final int quadCount;
    protected final TranslucentData sourceData;

    DynamicSorter(int quadCount, TranslucentData sourceData) {
        this.quadCount = quadCount;
        this.sourceData = sourceData;
    }

    abstract void writeSort(CombinedCameraPos cameraPos);

    @Override
    public void writeIndexBuffer(CombinedCameraPos cameraPos) {
        this.initBufferWithQuadLength(this.quadCount);
        this.writeSort(cameraPos);
    }

    public int getQuadCount() {
        return this.quadCount;
    }
    
    public int getResultSize() {
        return TranslucentData.quadCountToIndexBytes(this.quadCount);
    }
}
