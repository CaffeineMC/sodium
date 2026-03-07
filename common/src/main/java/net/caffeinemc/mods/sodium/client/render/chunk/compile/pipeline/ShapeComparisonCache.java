package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShapeComparisonCache {
    private static final int CACHE_SIZE = 512;

    private static final int ENTRY_ABSENT = -1;
    private static final int ENTRY_FALSE = 0;
    private static final int ENTRY_TRUE = 1;

    private final Object2IntLinkedOpenCustomHashMap<ShapeComparison> comparisonLookupTable;
    private final ShapeComparison cachedComparisonObject = new ShapeComparison();

    public ShapeComparisonCache() {
        this.comparisonLookupTable = new Object2IntLinkedOpenCustomHashMap<>(CACHE_SIZE, 0.5F, new ShapeComparison.ShapeComparisonStrategy());
        this.comparisonLookupTable.defaultReturnValue(ENTRY_ABSENT);
    }

    public static boolean isFullShape(VoxelShape selfShape) {
        return selfShape == Shapes.block();
    }

    public static boolean isEmptyShape(VoxelShape voxelShape) {
        return voxelShape == Shapes.empty() || voxelShape.isEmpty();
    }

    public boolean lookup(VoxelShape self, VoxelShape other) {
        return this.lookup(self, other, null);
    }

    public boolean lookup(VoxelShape self, VoxelShape other, VoxelShape constraint) {
        ShapeComparison comparison = this.cachedComparisonObject;
        comparison.self = self;
        comparison.other = other;
        comparison.constraint = constraint;

        // Entries at the cache are promoted to the top of the table when accessed
        // The entries at the bottom of the table are removed when it gets too large
        return switch (this.comparisonLookupTable.getAndMoveToFirst(comparison)) {
            case ENTRY_FALSE -> false;
            case ENTRY_TRUE -> true;
            default -> this.calculate(comparison);
        };
    }

    private boolean calculate(ShapeComparison comparison) {
        var self = comparison.self;

        // only consider part of the self shape where it is not occluded by the constraint
        if (comparison.constraint != null && !comparison.constraint.isEmpty()) {
            self = Shapes.join(comparison.self, comparison.constraint, BooleanOp.ONLY_FIRST);
        }

        boolean result = Shapes.joinIsNotEmpty(self, comparison.other, BooleanOp.ONLY_FIRST);

        // Remove entries while the table is too large
        while (this.comparisonLookupTable.size() >= CACHE_SIZE) {
            this.comparisonLookupTable.removeLastInt();
        }

        this.comparisonLookupTable.putAndMoveToFirst(comparison.copy(), (result ? ENTRY_TRUE : ENTRY_FALSE));

        return result;
    }

    private static final class ShapeComparison {
        private VoxelShape self, other, constraint;

        private ShapeComparison() {
        }

        private ShapeComparison(VoxelShape self, VoxelShape other, VoxelShape constraint) {
            this.self = self;
            this.other = other;
            this.constraint = constraint;
        }

        public static class ShapeComparisonStrategy implements Hash.Strategy<ShapeComparison> {
            @Override
            public int hashCode(ShapeComparison value) {
                int result = System.identityHashCode(value.self);
                result = 31 * result + System.identityHashCode(value.other);
                result = 31 * result + System.identityHashCode(value.constraint);

                return result;
            }

            @Override
            public boolean equals(ShapeComparison a, ShapeComparison b) {
                if (a == b) {
                    return true;
                }

                if (a == null || b == null) {
                    return false;
                }

                return a.self == b.self && a.other == b.other && a.constraint == b.constraint;
            }
        }

        public ShapeComparison copy() {
            return new ShapeComparison(this.self, this.other, this.constraint);
        }
    }
}
