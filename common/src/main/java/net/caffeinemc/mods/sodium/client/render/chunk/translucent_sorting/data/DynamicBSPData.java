package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPNode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPResult;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.UpdatedQuadsList;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
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

    private final int indexQuadCount;
    private final BSPNode rootNode;
    private final int generation;
    private UpdatedQuadsList updatedQuadsList;
    private final boolean neededQuadSplitting;

    private DynamicBSPData(SectionPos sectionPos, int inputQuadCount, BSPResult result, Vector3dc initialCameraPos, int generation) {
        super(sectionPos, inputQuadCount, result, initialCameraPos);
        this.rootNode = result.getRootNode();
        this.generation = generation;
        this.updatedQuadsList = result.getUpdatedQuadsList();
        this.neededQuadSplitting = this.updatedQuadsList != null;

        if (this.updatedQuadsList != null) {
            this.indexQuadCount = this.updatedQuadsList.getIndexQuadCount();
        } else {
            this.indexQuadCount = inputQuadCount;
        }
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
    public boolean oldDataMatches(TranslucentGeometryCollector collector, SortType sortType, TQuad[] quads) {
        // don't reuse data if we need to rewrite the mesh because of quad splitting
        return !this.meshesWereModified() && super.oldDataMatches(collector, sortType, quads);
    }

    @Override
    public int getIndexQuadCount() {
        return this.indexQuadCount;
    }

    @Override
    public DynamicSorter getSorter() {
        // release references to the modified quad list,
        // since we sort during the meshing task for the first time (in particular, when there was a non-null updated quad list)
        this.updatedQuadsList = null;

        return new DynamicBSPSorter(this.getIndexQuadCount()); // index quad count
    }

    @Override
    public UpdatedQuadsList getUpdatedQuads() {
        return this.updatedQuadsList;
    }

    @Override
    public boolean meshesWereModified() {
        return this.neededQuadSplitting;
    }

    public static DynamicBSPData fromMesh(CombinedCameraPos cameraPos, TQuad[] quads, SectionPos sectionPos,
                                          TranslucentData oldData, QuadSplittingMode quadSplittingMode) {
        BSPNode oldRoot = null;
        int generation = 0;
        boolean prepareNodeReuse = false;
        boolean allowNodeReuse = false;
        if (oldData instanceof DynamicBSPData oldBSPData) {
            generation = oldBSPData.generation + 1;
            oldRoot = oldBSPData.rootNode;

            // disallow making use of node reuse if quad splitting ended up being needed when the tree was originally built
            allowNodeReuse = !oldBSPData.neededQuadSplitting;

            // only enable partial updates after a certain number of generations
            // (times the section has been built)
            prepareNodeReuse = allowNodeReuse && generation >= NODE_REUSE_MIN_GENERATION;
        }
        var result = BSPNode.buildBSP(quads, sectionPos, oldRoot, prepareNodeReuse, allowNodeReuse, quadSplittingMode);

        var dynamicData = new DynamicBSPData(sectionPos, quads.length, result, cameraPos.getAbsoluteCameraPos(), generation);

        // prepare geometry planes for integration into GFNI triggering
        result.prepareIntegration();

        return dynamicData;
    }
}
