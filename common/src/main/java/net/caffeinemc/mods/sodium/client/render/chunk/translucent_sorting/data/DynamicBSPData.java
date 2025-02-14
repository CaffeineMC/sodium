package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPNode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPResult;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.core.SectionPos;
import org.joml.Vector3dc;

/**
 * Constructs a BSP tree of the quads and sorts them dynamically.
 * <p>
 * Triggering is performed when the BSP tree's partition planes are crossed in
 * any direction (bidirectional).
 */
public class DynamicBSPData extends DynamicData {
    private static final int NODE_REUSE_MIN_GENERATION = 1;

    private final BSPNode rootNode;
    private final int generation;
    private final BSPResult.UpdatedQuadIndexes updatedQuadIndexes;

    private DynamicBSPData(SectionPos sectionPos, int quadCount, BSPResult result, Vector3dc initialCameraPos, int generation) {
        super(sectionPos, quadCount, result, initialCameraPos);
        this.rootNode = result.getRootNode();
        this.generation = generation;
        this.updatedQuadIndexes = result.getUpdatedQuadIndexes();
    }

    private class DynamicBSPSorter extends DynamicSorter {
        private DynamicBSPSorter(int quadCount) {
            super(quadCount);
        }

        @Override
        void writeSort(CombinedCameraPos cameraPos, boolean initial) {
            DynamicBSPData.this.rootNode.collectSortedQuads(this.getIndexBuffer(), cameraPos.getRelativeCameraPos());
        }
    }

    @Override
    public boolean oldDataMatches(TranslucentGeometryCollector collector, SortType sortType, TQuad[] quads, int[] vertexCounts) {
        // don't reuse data if we need to rewrite the mesh because of quad splitting
        return !this.meshesWereModified() && super.oldDataMatches(collector, sortType, quads, vertexCounts);
    }

    @Override
    public Sorter getSorter() {
        return new DynamicBSPSorter(this.getQuadCount());
    }

    @Override
    public BSPResult.UpdatedQuadIndexes getUpdatedQuadIndexes() {
        return this.updatedQuadIndexes;
    }

    public static DynamicBSPData fromMesh(CombinedCameraPos cameraPos, TQuad[] quads, SectionPos sectionPos,
                                          TranslucentData oldData, ChunkMeshBufferBuilder translucentVertexBuffer) {
        BSPNode oldRoot = null;
        int generation = 0;
        boolean prepareNodeReuse = false;
        if (oldData instanceof DynamicBSPData oldBSPData) {
            generation = oldBSPData.generation + 1;
            oldRoot = oldBSPData.rootNode;

            // only enable partial updates after a certain number of generations
            // (times the section has been built)
            prepareNodeReuse = generation >= NODE_REUSE_MIN_GENERATION;
        }
        var result = BSPNode.buildBSP(quads, sectionPos, oldRoot, prepareNodeReuse, translucentVertexBuffer);

        var newQuadCount = quads.length + result.getAppendedQuadCount();
        var dynamicData = new DynamicBSPData(sectionPos, newQuadCount, result, cameraPos.getAbsoluteCameraPos(), generation);

        // prepare geometry planes for integration into GFNI triggering
        result.prepareIntegration();

        return dynamicData;
    }
}
