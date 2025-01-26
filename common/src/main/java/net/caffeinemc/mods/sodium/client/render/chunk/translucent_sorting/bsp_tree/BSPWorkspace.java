package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.QuadSplittingMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.FullTQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.core.SectionPos;
import org.joml.Vector3fc;

/**
 * The BSP workspace holds the state during the BSP building process. (see also
 * BSPSortState) It brings a number of fixed parameters and receives partition
 * planes to return as part of the final result.
 * 
 * Implementation note: Storing the multi partition node's interval points in a
 * global array instead of making a new one at each tree level doesn't appear to
 * have any performance benefit.
 */
class BSPWorkspace extends ObjectArrayList<TQuad> {
    final BSPResult result = new BSPResult();

    final SectionPos sectionPos;
    final boolean prepareNodeReuse;
    final QuadSplittingMode quadSplittingMode;
    private int remainingNewQuads = 0;

    private final ChunkMeshBufferBuilder translucentVertexBuffer;

    BSPWorkspace(TQuad[] quads, SectionPos sectionPos, boolean prepareNodeReuse, QuadSplittingMode quadSplittingMode, ChunkMeshBufferBuilder translucentVertexBuffer) {
        super(quads);
        this.sectionPos = sectionPos;
        this.prepareNodeReuse = prepareNodeReuse;
        this.quadSplittingMode = quadSplittingMode;
        this.translucentVertexBuffer = translucentVertexBuffer;

        if (quadSplittingMode.allowsSplitting()) {
            this.remainingNewQuads = quadSplittingMode.getMaxExtraQuads(quads.length);
        }
    }

    boolean canSplitQuads() {
        return this.remainingNewQuads > 0;
    }

    // TODO: better bidirectional triggering: integrate bidirectionality in GFNI if
    // top-level topo sorting isn't used anymore (and only use half as much memory
    // by not storing trigger planes twice)
    void addAlignedPartitionPlane(int axis, float distance) {
        this.result.addDoubleSidedAlignedPlane(this.sectionPos, axis, distance);
    }

    void addUnalignedPartitionPlane(Vector3fc planeNormal, float distance) {
        this.result.addDoubleSidedUnalignedPlane(this.sectionPos, planeNormal, distance);
    }

    int pushQuad(FullTQuad quad) {
        if (quad == null || quad.isInvalid()) {
            return -1;
        }

        this.remainingNewQuads--;

        this.translucentVertexBuffer.push(quad.getVertices(), DefaultMaterials.TRANSLUCENT);

        var index = this.size();
        this.add(quad);
        this.result.ensureUpdatedQuadIndexes().addAppendedQuadIndex(index);

        return index;
    }

    int updateQuad(FullTQuad quad, int quadIndex) {
        if (quad == null || quad.isInvalid()) {
            return -1;
        }

        this.translucentVertexBuffer.push(quad.getVertices(), DefaultMaterials.TRANSLUCENT);

        this.result.ensureUpdatedQuadIndexes().addModifiedQuadIndex(quadIndex);

        return quadIndex;
    }
}
