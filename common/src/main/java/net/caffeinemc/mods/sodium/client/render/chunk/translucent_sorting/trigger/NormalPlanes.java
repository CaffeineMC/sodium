package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.interval_tree.DoubleInterval;
import net.caffeinemc.mods.sodium.client.util.interval_tree.Interval.Bounded;
import net.minecraft.core.SectionPos;
import org.joml.Vector3fc;

import java.util.Arrays;

/**
 * NormalPlanes represents planes by a normal and a list of distances. Initially they're
 * stored in a hash set and later sorted for range queries.
 */
public class NormalPlanes {
    final FloatOpenHashSet relativeDistancesSet = new FloatOpenHashSet(16);

    final Vector3fc normal;
    final int alignedDirection;

    final SectionPos sectionPos;

    float[] relativeDistances; // relative to the base distance
    DoubleInterval distanceRange;
    long relDistanceHash;
    double baseDistance;

    private NormalPlanes(SectionPos sectionPos, Vector3fc normal, int alignedDirection) {
        this.sectionPos = sectionPos;

        this.normal = normal;
        this.alignedDirection = alignedDirection;
    }

    public NormalPlanes(SectionPos sectionPos, Vector3fc normal) {
        this(sectionPos, normal, ModelQuadFacing.UNASSIGNED_ORDINAL);
    }

    public NormalPlanes(SectionPos sectionPos, int alignedDirection) {
        this(sectionPos, ModelQuadFacing.ALIGNED_NORMALS[alignedDirection], alignedDirection);
    }

    public void addPlaneMember(float distance) {
        this.relativeDistancesSet.add(distance);
    }

    public void prepareIntegration() {
        // stop if already prepared
        if (this.relativeDistances != null) {
            throw new IllegalStateException("Already prepared");
        }

        // store the absolute face plane distances in an array
        var size = this.relativeDistancesSet.size();
        this.relativeDistances = new float[this.relativeDistancesSet.size()];
        int i = 0;
        for (var it = this.relativeDistancesSet.iterator(); it.hasNext(); ) {
            float relDistance = it.nextFloat();
            this.relativeDistances[i++] = relDistance;

            long distanceBits = Double.doubleToLongBits(relDistance);
            this.relDistanceHash ^= this.relDistanceHash * 31L + distanceBits;
        }

        // sort the array ascending
        Arrays.sort(this.relativeDistances);

        // make sure to use double-based dot product math here to prevent incorrect sort triggering
        this.baseDistance = MathUtil.floatDoubleDot(this.normal, this.sectionPos.minBlockX(), this.sectionPos.minBlockY(), this.sectionPos.minBlockZ());
        this.distanceRange = new DoubleInterval(
                this.relativeDistances[0] + this.baseDistance,
                this.relativeDistances[size - 1] + this.baseDistance,
                Bounded.CLOSED);
    }

    public void prepareAndInsert(Object2ReferenceMap<Vector3fc, float[]> distancesByNormal) {
        this.prepareIntegration();
        if (distancesByNormal != null) {
            distancesByNormal.put(this.normal, this.relativeDistances);
        }
    }
}
