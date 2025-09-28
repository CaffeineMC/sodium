package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateTypes;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.minecraft.util.Mth;

public class PendingTaskCollector implements OcclusionCuller.GraphOcclusionVisitor {
    public static final int SECTION_Y_MIN = -128; // used instead of baseOffsetY to accommodate all permissible y values (-2048 to 2048 blocks)

    // tunable parameters for the priority calculation.
    // each "gained" point means a reduction in the final priority score (lowest score processed first)
    static final float PENDING_TIME_FACTOR = -1.0f / 5_000_000_000.0f; // 1 point gained per 5s
    static final float WITHIN_FRUSTUM_BIAS = -3.0f; // points for being within the frustum
    static final float PROXIMITY_FACTOR = 3.0f; // penalty for being far away
    static final float CLOSE_DISTANCE = 50.0f; // distance at which another proximity bonus is applied
    static final float CLOSE_PROXIMITY_FACTOR = 0.6f; // penalty for being CLOSE_DISTANCE or farther away
    static final float INV_MAX_DISTANCE_CLOSE = CLOSE_PROXIMITY_FACTOR / CLOSE_DISTANCE;

    private final LongArrayList pendingTasks = new LongArrayList();

    protected final boolean isFrustumTested;
    protected final int baseOffsetX, baseOffsetY, baseOffsetZ;

    protected final int cameraX, cameraY, cameraZ;
    private final float invMaxDistance;
    private final long creationTime;

    public PendingTaskCollector(Viewport viewport, float buildDistance, boolean frustumTested) {
        this.creationTime = System.nanoTime();
        this.isFrustumTested = frustumTested;
        var offsetDistance = Mth.ceil(buildDistance / 16.0f) + 1;

        // the offset applied to section coordinates to encode their position in the octree
        var sectionPos = viewport.getChunkCoord();
        var cameraSectionX = sectionPos.getX();
        var cameraSectionY = sectionPos.getY();
        var cameraSectionZ = sectionPos.getZ();
        this.baseOffsetX = cameraSectionX - offsetDistance;
        this.baseOffsetY = cameraSectionY - offsetDistance;
        this.baseOffsetZ = cameraSectionZ - offsetDistance;

        this.invMaxDistance = PROXIMITY_FACTOR / buildDistance;

        if (frustumTested) {
            var blockPos = viewport.getBlockCoord();
            this.cameraX = blockPos.getX();
            this.cameraY = blockPos.getY();
            this.cameraZ = blockPos.getZ();
        } else {
            this.cameraX = (cameraSectionX << 4);
            this.cameraY = (cameraSectionY << 4);
            this.cameraZ = (cameraSectionZ << 4);
        }
    }

    @Override
    public void visit(RenderSection section) {
        this.checkForTask(section);
    }

    @Override
    public long getAngleVisibilityMask(Viewport viewport, RenderSection section) {
        if (this.isFrustumTested) {
            return OcclusionCuller.GraphOcclusionVisitor.super.getAngleVisibilityMask(viewport, section);
        }

        return calculateSectionAngleVisibilityMask(viewport, section, 1);
    }

    protected static long calculateSectionAngleVisibilityMask(Viewport viewport, RenderSection section, int width) {
        // compare the origin and the section centers
        var origin = viewport.getChunkCoord();
        var dx = Math.abs(origin.minBlockX() + 8 - section.getCenterX());
        var dy = Math.abs(origin.minBlockY() + 8 - section.getCenterY());
        var dz = Math.abs(origin.minBlockZ() + 8 - section.getCenterZ());

        // in a pair da > db both distances can be up to 8 greater or 8 smaller.
        // since we only want to apply occlusion if every combination satisfies the occlusion condition,
        // we would need to do combinations of da -/+ 8 > db -/+ 8, which is equivalent to the worst case da > db + 16
        var margin = 32 * width - 16;
        var angleOcclusionMask = 0L;
        if (dx > dy + margin || dz > dy + margin) {
            angleOcclusionMask |= UP_DOWN_OCCLUDED;
        }
        if (dx > dz + margin || dy > dz + margin) {
            angleOcclusionMask |= NORTH_SOUTH_OCCLUDED;
        }
        if (dy > dx + margin || dz > dx + margin) {
            angleOcclusionMask |= WEST_EAST_OCCLUDED;
        }

        return ~angleOcclusionMask;
    }

    @Override
    public int getDirectionSets(Viewport viewport, RenderSection section) {
        if (this.isFrustumTested) {
            return OcclusionCuller.GraphOcclusionVisitor.super.getDirectionSets(viewport, section);
        }

        return calculateDirectionSets(viewport, section, 1);
    }

    protected static int calculateDirectionSets(Viewport viewport, RenderSection section, int width) {
        var origin = viewport.getChunkCoord();
        var minX = origin.minBlockX();
        var minY = origin.minBlockY();
        var minZ = origin.minBlockZ();

        var posMargin = 16 * width;
        var negMargin = 16 * (width - 1);

        // determine which base perspectives need to be combined based on the camera position relative to the section.
        // these bitmasks correspond to the base directions in DirectionalVisGraph.DIRECTION_SETS
        int directionSetsX = 0;
        if (minX + posMargin >= section.getOriginX()) {
            directionSetsX = 0b00001111;
        }
        if (minX - negMargin <= section.getOriginX() + 16) {
            directionSetsX |= 0b11110000;
        }

        int directionSetsZ = 0;
        if (minZ + posMargin >= section.getOriginZ()) {
            directionSetsZ = 0b00110011;
        }
        if (minZ - negMargin <= section.getOriginZ() + 16) {
            directionSetsZ |= 0b11001100;
        }

        int directionSetsY = 0;
        if (minY + posMargin >= section.getOriginY()) {
            directionSetsY = 0b01010101;
        }
        if (minY - negMargin <= section.getOriginY() + 16) {
            directionSetsY |= 0b10101010;
        }

        return directionSetsX & directionSetsY & directionSetsZ;
    }

    protected void checkForTask(RenderSection section) {
        int type = section.getPendingUpdate();

        // collect tasks even if they're important, whether they're actually important is decided later
        if (type != 0) {
            this.addPendingSection(section, type);
        }
    }

    protected void addPendingSection(RenderSection section, int type) {
        // start with a base priority value, lowest priority of task gets processed first
        float priority = getSectionPriority(section, type);

        // encode the absolute position of the section
        var localX = section.getChunkX() - this.baseOffsetX;
        var localY = section.getChunkY() - SECTION_Y_MIN;
        var localZ = section.getChunkZ() - this.baseOffsetZ;
        long taskCoordinate = (long) (localX & 0b1111111111) << 20 | (long) (localY & 0b1111111111) << 10 | (long) (localZ & 0b1111111111);

        // encode the priority and the section position into a single long such that all parts can be later decoded
        this.pendingTasks.add((long) MathUtil.floatToComparableInt(priority) << 32 | taskCoordinate);
    }

    private float getSectionPriority(RenderSection section, int type) {
        float priority = ChunkUpdateTypes.getPriorityValue(type);

        // calculate the relative distance to the camera
        // alternatively: var distance = deltaX + deltaY + deltaZ;
        var deltaX = Math.abs(section.getCenterX() - this.cameraX);
        var deltaY = Math.abs(section.getCenterY() - this.cameraY);
        var deltaZ = Math.abs(section.getCenterZ() - this.cameraZ);
        var distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        priority += distance * this.invMaxDistance; // distance / maxDistance * PROXIMITY_FACTOR
        priority += Math.max(distance, CLOSE_DISTANCE) * INV_MAX_DISTANCE_CLOSE;

        // tasks that have been waiting for longer are more urgent
        var taskPendingTimeNanos = this.creationTime - section.getPendingUpdateSince();
        priority += taskPendingTimeNanos * PENDING_TIME_FACTOR; // upgraded by one point every second

        // explain how priority was calculated
//        System.out.println("Priority " + priority + " from: distance " + distance + " = " + (distance * this.invMaxDistance) +
//                ", time " + taskPendingTimeNanos + " = " + (taskPendingTimeNanos * PENDING_TIME_FACTOR) +
//                ", type " + type + " = " + type.getPriorityValue() +
//                ", frustum " + this.isFrustumTested + " = " + (this.isFrustumTested ? WITHIN_FRUSTUM_BIAS : 0));

        return priority;
    }

    public static float decodePriority(long encoded) {
        return MathUtil.comparableIntToFloat((int) (encoded >>> 32));
    }

    public DeferredTaskList getPendingTaskLists() {
        return DeferredTaskList.createHeapCopyOf(this.pendingTasks, this.creationTime, this.isFrustumTested, this.baseOffsetX, this.baseOffsetZ);
    }

}
