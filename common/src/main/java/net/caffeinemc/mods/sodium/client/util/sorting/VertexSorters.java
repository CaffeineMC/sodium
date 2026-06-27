package net.caffeinemc.mods.sodium.client.util.sorting;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

public class VertexSorters {
    public static VertexSortingExtended distance(float x, float y, float z) {
        if (x == 0.0f && y == 0.0f && z == 0.0f) {
            return SortByDistanceToOrigin.INSTANCE;
        }

        return new SortByDistanceToPoint(x, y, z);
    }

    public static VertexSortingExtended orthographicZ() {
        return SortByOrthographicZ.INSTANCE;
    }

    // Slow, should only be used when none of the other classes apply.
    public static VertexSortingExtended fallback(VertexSorting.DistanceFunction metric) {
        return new SortByFallback(metric);
    }

    private abstract static class AbstractSorter implements VertexSortingExtended {
        @Override
        public final int @NonNull [] sort(CompactVectorArray centroids) {
            final int length = centroids.size();
            final var keys = new int[length];
            final var perm = new int[length];

            for (int index = 0; index < length; index++) {
                keys[index] = ~MathUtil.floatToComparableInt(this.applyMetric(centroids.getX(index), centroids.getY(index), centroids.getZ(index)));
                perm[index] = index;
            }

            RadixSort.sortIndirect(perm, keys, true);

            return perm;
        }
    }

    private static class SortByDistanceToPoint extends AbstractSorter {
        private final float x, y, z;

        private SortByDistanceToPoint(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public float applyMetric(float x, float y, float z) {
            float dx = this.x - x;
            float dy = this.y - y;
            float dz = this.z - z;

            return (dx * dx) + (dy * dy) + (dz * dz);
        }
    }

    private static class SortByDistanceToOrigin extends AbstractSorter {
        private static final SortByDistanceToOrigin INSTANCE = new SortByDistanceToOrigin();

        @Override
        public float applyMetric(float x, float y, float z) {
            return (x * x) + (y * y) + (z * z);
        }
    }

    private static class SortByOrthographicZ extends AbstractSorter {
        private static final SortByOrthographicZ INSTANCE = new SortByOrthographicZ();

        @Override
        public float applyMetric(float x, float y, float z) {
            return -z;
        }
    }

    private static class SortByFallback extends AbstractSorter {
        private final DistanceFunction function;
        private final Vector3f scratch = new Vector3f();

        private SortByFallback(DistanceFunction function) {
            this.function = function;
        }

        @Override
        public float applyMetric(float x, float y, float z) {
            return this.function.apply(this.scratch.set(x, y, z));
        }
    }
}
