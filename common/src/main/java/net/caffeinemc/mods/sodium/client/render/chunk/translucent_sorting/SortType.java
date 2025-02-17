package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting;

/**
 * What type of sorting to use for a section. Calculated by a heuristic after
 * building a section.
 *
 * Invariant: !(needsDirectionMixing && allowSliceReordering)
 */
public enum SortType {
    /**
     * The section is fully empty, no index buffer is needed.
     */
    EMPTY_SECTION(false, true),

    /**
     * The section has no translucent geometry, no index buffer is needed.
     */
    NO_TRANSLUCENT(false, true),

    /**
     * No sorting is required and the sort order doesn't matter.
     */
    NONE(false, true),

    /**
     * There is only one sort order. No active sorting is required, except for an initial
     * sort where quads of each facing are sorted according to their distances in
     * regard to their normal.
     */
    STATIC_NORMAL_RELATIVE(false, false),

    /**
     * There is only one sort order and not active sorting is required, but
     * determining the static sort order involves doing a toplogical sort of the
     * quads.
     */
    STATIC_TOPO(true, false),

    /**
     * There are multiple sort orders. Sorting is required every time GFNI triggers
     * this section.
     */
    DYNAMIC(true, false);

    public final boolean needsDirectionMixing;
    public final boolean allowSliceReordering;

    SortType(boolean needsDirectionMixing, boolean allowSliceReordering) {
        this.needsDirectionMixing = needsDirectionMixing;
        this.allowSliceReordering = allowSliceReordering;

        if (needsDirectionMixing && allowSliceReordering) {
            throw new IllegalArgumentException("Invalid sort type");
        }
    }
}
