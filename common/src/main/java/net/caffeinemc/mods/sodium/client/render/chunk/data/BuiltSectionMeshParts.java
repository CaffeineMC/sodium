package net.caffeinemc.mods.sodium.client.render.chunk.data;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

/**
 * The array of vertex segments is structured as follows:
 * - It consists of 2 * ModelQuadFacing.COUNT ints.
 * - The first and every second int after that are vertex counts.
 * - The second and every second int after that are the ModelQuadFacing index that the preceding count applies to.
 * - If the vertex count is zero, the segment is not used and reading the facing index is undefined behavior.
 * - The array of vertex segments starts with some number of filled segments, followed by empty segments for the rest of the fixed size.
 */
public class BuiltSectionMeshParts {
    private final int[] vertexSegments;
    private final NativeBuffer buffer;

    public BuiltSectionMeshParts(NativeBuffer buffer, int[] vertexCounts) {
        this.vertexSegments = vertexCounts;
        this.buffer = buffer;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    public int[] getVertexSegments() {
        return this.vertexSegments;
    }

    public int[] computeVertexCounts() {
        var vertexCounts = new int[ModelQuadFacing.COUNT];

        for (int i = 0; i < this.vertexSegments.length; i += 2) {
            var vertexCount = this.vertexSegments[i];
            if (vertexCount == 0) {
                continue; // Skip non-present segments
            }
            vertexCounts[this.vertexSegments[i + 1]] = vertexCount;
        }

        return vertexCounts;
    }
}
