package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad.TQuad;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.sorting.RadixSort;
import net.minecraft.core.SectionPos;

/**
 * Static normal relative sorting orders quads by the dot product of their
 * normal and position. (referred to as "distance" throughout the code)
 *
 * Unlike sorting by distance, which is descending for translucent rendering to
 * be correct, sorting by dot product is ascending instead.
 */
public class StaticNormalRelativeData extends SplitDirectionData {
    private Sorter sorterOnce;

    public StaticNormalRelativeData(SectionPos sectionPos, int[] vertexCounts, int quadCount) {
        super(sectionPos, vertexCounts, quadCount);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_NORMAL_RELATIVE;
    }

    @Override
    public Sorter getSorter() {
        var sorter = this.sorterOnce;
        if (sorter == null) {
            throw new IllegalStateException("Sorter already used!");
        }
        this.sorterOnce = null;
        return sorter;
    }

    private static StaticNormalRelativeData fromDoubleUnaligned(int[] vertexCounts, TQuad[] quads, SectionPos sectionPos) {
        var snrData = new StaticNormalRelativeData(sectionPos, vertexCounts, quads.length);
        var sorter = new StaticSorter(quads.length);
        snrData.sorterOnce = sorter;
        var indexBuffer = sorter.getIntBuffer();

        if (quads.length <= 1) {
            // Avoid allocations when there is nothing to sort.
            TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
        } else {
            final var keys = new int[quads.length];
            final var perm = new int[quads.length];

            for (int q = 0; q < quads.length; q++) {
                keys[q] = MathUtil.floatToComparableInt(quads[q].getAccurateDotProduct());
                perm[q] = q;
            }

            RadixSort.sortIndirect(perm, keys, false);

            for (int i = 0; i < quads.length; i++) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, perm[i]);
            }
        }

        return snrData;
    }

    /**
     * Important: The vertex indexes must start at zero for each facing.
     */
    private static StaticNormalRelativeData fromMixed(int[] vertexCounts,
                                                      TQuad[] quads, SectionPos sectionPos) {
        var snrData = new StaticNormalRelativeData(sectionPos, vertexCounts, quads.length);
        var sorter = new StaticSorter(quads.length);
        snrData.sorterOnce = sorter;
        var indexBuffer = sorter.getIntBuffer();

        var maxQuadCount = 0;

        for (var vertexCount : vertexCounts) {
            if (vertexCount != -1) {
                var quadCount = TranslucentData.vertexCountToQuadCount(vertexCount);
                maxQuadCount = Math.max(maxQuadCount, quadCount);
            }
        }

        // The quad index is used to keep track of the position in the quad array.
        // This is necessary because the emitted quad indexes in each facing start at zero,
        // but the quads are stored in a single continuously indexed array.
        int quadIndex = 0;
        for (var vertexCount : vertexCounts) {
            if (vertexCount == -1 || vertexCount == 0) {
                continue;
            }

            int count = TranslucentData.vertexCountToQuadCount(vertexCount);

            if (count == 1) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
                quadIndex++;
            } else {
                final var keys = new int[count];
                final var perm = new int[count];

                for (int idx = 0; idx < count; idx++) {
                    keys[idx] = MathUtil.floatToComparableInt(quads[quadIndex++].getAccurateDotProduct());
                    perm[idx] = idx;
                }

                RadixSort.sortIndirect(perm, keys, false);

                for (int idx = 0; idx < count; idx++) {
                    TranslucentData.writeQuadVertexIndexes(indexBuffer, perm[idx]);
                }
            }
        }

        return snrData;
    }

    public static StaticNormalRelativeData fromMesh(int[] vertexCounts,
            TQuad[] quads, SectionPos sectionPos, boolean isDoubleUnaligned) {
        if (isDoubleUnaligned) {
            return fromDoubleUnaligned(vertexCounts, quads, sectionPos);
        } else {
            return fromMixed(vertexCounts, quads, sectionPos);
        }
    }
}
