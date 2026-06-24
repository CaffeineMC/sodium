package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;

public abstract class DynamicSorter extends PresentSorter {
    private final int quadCount;
    protected final TranslucentData sourceData;
    private final GeometryPlanes geometryPlanes;

    DynamicSorter(int quadCount, DynamicData sourceData, GeometryPlanes geometryPlanes) {
        this.quadCount = quadCount;
        this.sourceData = sourceData;
        this.geometryPlanes = geometryPlanes;
    }

    abstract void writeSort(CombinedCameraPos cameraPos);

    public GeometryPlanes getGeometryPlanes() {
        return this.geometryPlanes;
    }

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
