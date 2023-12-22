package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * With this sort type the section's translucent quads can be rendered in any
 * order. However, they do need to be rendered with some index buffer, so that
 * vertices are assembled into quads. Since the sort order doesn't matter, all
 * sections with this sort type can share the same data in the index buffer.
 * 
 * NOTE: A possible optimization would be to share the buffer for unordered
 * translucent sections on the CPU and on the GPU. It would essentially be the
 * same as SharedQuadIndexBuffer, but it has to be compatible with sections in
 * the same region using custom index buffers which makes the management
 * complicated. The shared buffer would be a member amongst the other non-shared
 * buffer segments and would need to be resized when a larger section wants to
 * use it.
 */
public class AnyOrderData extends SplitDirectionData {
    AnyOrderData(ChunkSectionPos sectionPos, NativeBuffer buffer, VertexRange[] ranges) {
        super(sectionPos, buffer, ranges);
    }

    @Override
    public SortType getSortType() {
        return SortType.NONE;
    }

    public static AnyOrderData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, ChunkSectionPos sectionPos, NativeBuffer buffer) {
        buffer = PresentTranslucentData.nativeBufferForQuads(buffer, quads);
        var indexBuffer = buffer.getDirectBuffer().asIntBuffer();
        var counter = 0;
        var lastFacing = quads[0].getFacing();
        for (int i = 0; i < quads.length; i++) {
            var currentFacing = quads[i].getFacing();
            if (i > 0 && currentFacing != lastFacing) {
                counter = 0;
            }
            lastFacing = currentFacing;
            TranslucentData.writeQuadVertexIndexes(indexBuffer, counter++);
        }
        return new AnyOrderData(sectionPos, buffer, translucentMesh.getVertexRanges());
    }
}