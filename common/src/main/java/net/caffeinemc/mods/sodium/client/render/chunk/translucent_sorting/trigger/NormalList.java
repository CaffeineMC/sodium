package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.interval_tree.DoubleInterval;
import net.caffeinemc.mods.sodium.client.util.interval_tree.Interval;
import net.caffeinemc.mods.sodium.client.util.interval_tree.Interval.Bounded;
import net.caffeinemc.mods.sodium.client.util.interval_tree.IntervalTree;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import java.util.Collection;

/**
 * A normal list contains all the face planes that have the same normal.
 */
public class NormalList {
    /**
     * Size threshold after which group sets in {@link #groupsByInterval} are
     * replaced with hash sets to improve update performance.
     */
    private static final int HASH_SET_THRESHOLD = 20;

    /**
     * Size threshold under which group sets in {@link #groupsByInterval} are
     * downgraded to array sets to reduce memory usage.
     */
    private static final int ARRAY_SET_THRESHOLD = 10;

    /**
     * The normal of this normal list.
     */
    private final Vector3fc normal;
    private final int alignedDirection;

    /**
     * An interval tree of group intervals. Since this only stores intervals, the
     * stored intervals are mapped to groups in a separate hashmap.
     */
    private final IntervalTree<Double> intervalTree = new IntervalTree<>();

    /**
     * A separate hashmap of groups. This is what actually stores the groups since
     * the interval tree just contains intervals.
     */
    private final Object2ReferenceOpenHashMap<DoubleInterval, Collection<Group>> groupsByInterval = new Object2ReferenceOpenHashMap<>();

    /**
     * A hashmap from chunk sections to groups. This is for finding groups during
     * updates.
     */
    private final Long2ReferenceOpenHashMap<Group> groupsBySection = new Long2ReferenceOpenHashMap<>();

    /**
     * Constructs a new normal list with the given unit normal vector and aligned
     * normal index.
     *
     * @param normal The unit normal vector
     */
    NormalList(Vector3fc normal, int alignedDirection) {
        this.normal = normal;
        this.alignedDirection = alignedDirection;
    }

    public Vector3fc getNormal() {
        return this.normal;
    }

    public boolean isAligned() {
        return this.alignedDirection != ModelQuadFacing.UNASSIGNED_ORDINAL;
    }

    public int getAlignedDirection() {
        return this.alignedDirection;
    }

    void processMovement(SortTriggering ts, CameraMovement movement) {
        // calculate the distance range of the movement with respect to the normal
        double start = MathUtil.floatDoubleDot(this.normal, movement.start());
        double end = MathUtil.floatDoubleDot(this.normal, movement.end());

        // stop if the movement is reverse in regard to the normal
        // since this means it's moving against the normal
        if (start >= end) {
            return;
        }

        // perform the interval query on the group intervals and resolve each interval
        // to the collection of groups it maps to
        var interval = new DoubleInterval(start, end, Bounded.CLOSED);
        for (Interval<Double> groupInterval : this.intervalTree.query(interval)) {
            for (Group group : this.groupsByInterval.get(groupInterval)) {
                group.triggerRange(ts, start, end);
            }
        }
    }

    void processCatchup(SortTriggering ts, CameraMovement movement, long sectionPos) {
        double start = MathUtil.floatDoubleDot(this.normal, movement.start());
        double end = MathUtil.floatDoubleDot(this.normal, movement.end());

        if (start >= end) {
            return;
        }

        var group = this.groupsBySection.get(sectionPos);
        if (group != null) {
            group.triggerRange(ts, start, end);
        }
    }

    private void removeGroupInterval(Group group) {
        var groups = this.groupsByInterval.get(group.distances);
        if (groups != null) {
            groups.remove(group);
            if (groups.isEmpty()) {
                this.groupsByInterval.remove(group.distances);

                // only remove from the interval tree if no other sections are also using it
                this.intervalTree.remove(group.distances);
            } else if (groups.size() <= ARRAY_SET_THRESHOLD) {
                groups = new ReferenceArraySet<>(groups);
                this.groupsByInterval.put(group.distances, groups);
            }
        }
    }

    private void addGroupInterval(Group group) {
        var groups = this.groupsByInterval.get(group.distances);
        if (groups == null) {
            groups = new ReferenceArraySet<>();
            this.groupsByInterval.put(group.distances, groups);

            // only add to the interval tree if it's a new interval
            this.intervalTree.add(group.distances);
        } else if (groups.size() >= HASH_SET_THRESHOLD) {
            groups = new ReferenceLinkedOpenHashSet<>(groups);
            this.groupsByInterval.put(group.distances, groups);
        }
        groups.add(group);
    }

    boolean hasSection(long sectionPos) {
        return this.groupsBySection.containsKey(sectionPos);
    }

    boolean isEmpty() {
        return this.groupsBySection.isEmpty();
    }

    void addSection(NormalPlanes normalPlanes, long sectionPos) {
        var group = new Group(normalPlanes);

        this.groupsBySection.put(sectionPos, group);
        this.addGroupInterval(group);
    }

    void removeSection(long sectionPos) {
        Group group = this.groupsBySection.remove(sectionPos);
        if (group != null) {
            this.removeGroupInterval(group);
        }
    }

    void updateSection(NormalPlanes normalPlanes, long sectionPos) {
        Group group = this.groupsBySection.get(sectionPos);

        // only update on changes to translucent geometry
        if (group.normalPlanesEquals(normalPlanes)) {
            // don't update if they are the same
            return;
        }

        this.removeGroupInterval(group);
        group.replaceWith(normalPlanes);
        this.addGroupInterval(group);
    }

    public static boolean queryRange(float[] sortedDistances, float start, float end) {
        // test that there is actually an entry in the query range
        int result = FloatArrays.binarySearch(sortedDistances, start);
        if (result < 0) {
            // recover the insertion point
            int insertionPoint = -result - 1;
            if (insertionPoint >= sortedDistances.length) {
                // no entry in the query range
                return false;
            }

            // check if the entry at the insertion point, which is the next one greater than
            // the start value, is less than or equal to the end value
            if (sortedDistances[insertionPoint] <= end) {
                // there is an entry in the query range
                return true;
            }
        } else {
            // exact match, trigger
            return true;
        }
        return false;
    }
}
