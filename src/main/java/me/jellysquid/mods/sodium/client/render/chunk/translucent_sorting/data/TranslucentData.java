package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.minecraft.core.SectionPos;

/**
 * The base class for all types of translucent data. Subclasses are generated by
 * the geometry collector after the section is built.
 */
public abstract class TranslucentData {
    public static final int INDICES_PER_QUAD = 6;
    public static final int VERTICES_PER_QUAD = 4;
    public static final int BYTES_PER_INDEX = 4;
    public static final int BYTES_PER_QUAD = INDICES_PER_QUAD * BYTES_PER_INDEX;

    public final SectionPos sectionPos;

    TranslucentData(SectionPos sectionPos) {
        this.sectionPos = sectionPos;
    }

    public abstract SortType getSortType();

    public boolean retainAfterUpload() {
        return false;
    }

    public void delete() {
    }

    public void sortOnTrigger(Vector3fc cameraPos) {
        // no-op for other translucent data than dynamic
    }

    /**
     * Prepares the translucent data for triggering of the given type. This is run
     * on the main thread before a sort task is scheduled.
     * 
     * @param isAngleTrigger Whether the trigger is an angle trigger
     */
    public void prepareTrigger(boolean isAngleTrigger) {
        // no-op for other translucent data than GFNI dynamic
    }

    public static int vertexCountToQuadCount(int vertexCount) {
        return vertexCount / VERTICES_PER_QUAD;
    }

    public static int quadCountToIndexBytes(int quadCount) {
        return quadCount * BYTES_PER_QUAD;
    }

    public static int indexBytesToQuadCount(int indexBytes) {
        return indexBytes / BYTES_PER_QUAD;
    }

    public static void writeQuadVertexIndexes(IntBuffer intBuffer, int quadIndex) {
        int vertexOffset = quadIndex * VERTICES_PER_QUAD;

        intBuffer.put(vertexOffset + 0);
        intBuffer.put(vertexOffset + 1);
        intBuffer.put(vertexOffset + 2);

        intBuffer.put(vertexOffset + 2);
        intBuffer.put(vertexOffset + 3);
        intBuffer.put(vertexOffset + 0);
    }

    public static void writeQuadVertexIndexes(IntBuffer intBuffer, int[] quadIndexes) {
        for (int quadIndexPos = 0; quadIndexPos < quadIndexes.length; quadIndexPos++) {
            writeQuadVertexIndexes(intBuffer, quadIndexes[quadIndexPos]);
        }
    }

    static VertexRange getUnassignedVertexRange(BuiltSectionMeshParts translucentMesh) {
        VertexRange range = translucentMesh.getVertexRanges()[ModelQuadFacing.UNASSIGNED.ordinal()];

        if (range == null) {
            throw new IllegalStateException("No unassigned data in mesh");
        }

        return range;
    }
}
