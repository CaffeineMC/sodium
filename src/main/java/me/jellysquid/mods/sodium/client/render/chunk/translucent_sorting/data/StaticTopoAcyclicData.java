package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.core.SectionPos;

/**
 * Static topo acyclic sorting uses the topo sorting algorithm but only if it's
 * possible to sort without dynamic triggering, meaning the sort order never
 * needs to change.
 */
public class StaticTopoAcyclicData extends MixedDirectionData {
    StaticTopoAcyclicData(SectionPos sectionPos, NativeBuffer buffer, VertexRange range) {
        super(sectionPos, buffer, range);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO;
    }

    public static StaticTopoAcyclicData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, SectionPos sectionPos, NativeBuffer buffer) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var indexBuffer = buffer.getDirectBuffer().asIntBuffer();

        if (!TopoGraphSorting.topoSortDepthFirstCyclic(indexBuffer, quads, null, null)) {
            return null;
        }

        return new StaticTopoAcyclicData(sectionPos, buffer, range);
    }
}
