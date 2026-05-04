package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkUpdateTypes;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.minecraft.world.level.Level;

public class TaskCollectingTree extends SectionTree {
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
    private final float invMaxDistance;
    private final long creationTime;

    public TaskCollectingTree(Viewport viewport, float buildDistance, int frame, CullType cullType, Level level) {
        super(viewport, buildDistance, frame, cullType, level);

        this.creationTime = System.nanoTime();
        this.invMaxDistance = PROXIMITY_FACTOR / buildDistance;
    }

    @Override
    public void visit(RenderSection section, boolean inFrustum) {
        super.visit(section, inFrustum);

        int type = section.getPendingUpdate();

        // collect tasks even if they're important, whether they're actually important is decided later
        if (type != 0) {
            this.addPendingSection(section, type, inFrustum);
        }
    }

    protected void addPendingSection(RenderSection section, int type, boolean inFrustum) {
        // start with a base priority value, lowest priority of task gets processed first
        float priority = getSectionPriority(section, type, inFrustum);

        // encode the absolute position of the section
        var localX = section.getChunkX() - this.baseOffsetX;
        var localY = section.getChunkY() - SECTION_Y_MIN;
        var localZ = section.getChunkZ() - this.baseOffsetZ;
        long taskCoordinate = (long) (localX & 0b1111111111) << 20 | (long) (localY & 0b1111111111) << 10 | (long) (localZ & 0b1111111111);

        // encode the priority and the section position into a single long such that all parts can be later decoded
        this.pendingTasks.add((long) MathUtil.floatToComparableInt(priority) << 32 | taskCoordinate);
    }

    private float getSectionPriority(RenderSection section, int type, boolean inFrustum) {
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

        if (inFrustum) {
            priority += WITHIN_FRUSTUM_BIAS;
        }

        return priority;
    }

    public DeferredTaskList getPendingTaskLists() {
        return DeferredTaskList.createHeapCopyOf(this.pendingTasks, this.baseOffsetX, this.baseOffsetZ);
    }
}
