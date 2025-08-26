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
public class StaticNormalRelativeData extends PresentTranslucentData {
    private Sorter sorterOnce;

    public StaticNormalRelativeData(SectionPos sectionPos, int inputQuadCount) {
        super(sectionPos, inputQuadCount);
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

    private static StaticNormalRelativeData fromDoubleUnaligned(TQuad[] quads, SectionPos sectionPos) {
        var snrData = new StaticNormalRelativeData(sectionPos, quads.length);
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
    private static StaticNormalRelativeData fromMixed(int[] meshFacingCounts,
                                                      TQuad[] quads, SectionPos sectionPos) {
        var snrData = new StaticNormalRelativeData(sectionPos, quads.length);
        var sorter = new StaticSorter(quads.length);
        snrData.sorterOnce = sorter;
        var indexBuffer = sorter.getIntBuffer();

        var maxQuadCount = 0;

        for (var quadCount : meshFacingCounts) {
            if (quadCount != -1) {
                maxQuadCount = Math.max(maxQuadCount, quadCount);
            }
        }

        // The quad index is used to keep track of the position in the quad array.
        // This is necessary because the emitted quad indexes in each facing start at zero,
        // but the quads are stored in a single continuously indexed array.
        int quadIndex = 0;
        for (var quadCount : meshFacingCounts) {
            if (quadCount == -1 || quadCount == 0) {
                continue;
            }

            if (quadCount == 1) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
                quadIndex++;
            } else {
                final var keys = new int[quadCount];
                final var perm = new int[quadCount];

                for (int idx = 0; idx < quadCount; idx++) {
                    keys[idx] = MathUtil.floatToComparableInt(quads[quadIndex++].getAccurateDotProduct());
                    perm[idx] = idx;
                }

                RadixSort.sortIndirect(perm, keys, false);

                for (int idx = 0; idx < quadCount; idx++) {
                    TranslucentData.writeQuadVertexIndexes(indexBuffer, perm[idx]);
                }
            }
        }

        return snrData;
    }

    public static StaticNormalRelativeData fromMesh(int[] meshFacingCounts,
            TQuad[] quads, SectionPos sectionPos, boolean isDoubleUnaligned) {
        if (isDoubleUnaligned) {
            return fromDoubleUnaligned(quads, sectionPos);
        } else {
            return fromMixed(meshFacingCounts, quads, sectionPos);
        }
    }
}
