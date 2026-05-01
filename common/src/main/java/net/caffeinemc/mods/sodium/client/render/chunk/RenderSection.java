package net.caffeinemc.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.MeshResultSize;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJob;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.GraphDirection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.GraphDirectionSet;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection {
    // Render Region State
    private final RenderRegion region;
    private final int sectionIndex;

    // Chunk Section State
    private final int chunkX, chunkY, chunkZ;

    // Occlusion Culling State
    private long[] visibilityData = null;

    private int incomingDirectionsWide;
    private int incomingDirectionsRegular;
    private int incomingDirectionsLocal;
    private int searchToken = -1;
    private long allowedAngles; // 60-bit packed quantized min/max allowed angles, 0-9 minXY, 10-19 maxXY, etc.

    private int adjacentMask;
    public RenderSection
            adjacentDown,
            adjacentUp,
            adjacentNorth,
            adjacentSouth,
            adjacentWest,
            adjacentEast;

    // Rendering State
    @Nullable
    private TranslucentData translucentData;

    // Update State
    private final List<ChunkJob> runningJobs = new ReferenceArrayList<>(2);
    private long lastMeshResultSize = MeshResultSize.NO_DATA;
    @Nullable
    private ChunkBuildOutput pendingBuildOutput;
    private int frameOfBuildSubmit;
    @Nullable
    private ChunkSortOutput pendingDynamicSortOutput;
    private int frameOfSortSubmit;

    private int pendingUpdateType;
    private long pendingUpdateSince;

    // Lifetime state
    private boolean disposed;

    public RenderSection(RenderRegion region, int chunkX, int chunkY, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int rX = this.getChunkX() & RenderRegion.REGION_WIDTH_M;
        int rY = this.getChunkY() & RenderRegion.REGION_HEIGHT_M;
        int rZ = this.getChunkZ() & RenderRegion.REGION_LENGTH_M;

        this.sectionIndex = LocalSectionIndex.pack(rX, rY, rZ);

        this.region = region;
    }

    public RenderSection getAdjacent(int direction) {
        return switch (direction) {
            case GraphDirection.DOWN -> this.adjacentDown;
            case GraphDirection.UP -> this.adjacentUp;
            case GraphDirection.NORTH -> this.adjacentNorth;
            case GraphDirection.SOUTH -> this.adjacentSouth;
            case GraphDirection.WEST -> this.adjacentWest;
            case GraphDirection.EAST -> this.adjacentEast;
            default -> null;
        };
    }

    public void setAdjacentNode(int direction, RenderSection node) {
        if (node == null) {
            this.adjacentMask &= ~GraphDirectionSet.of(direction);
        } else {
            this.adjacentMask |= GraphDirectionSet.of(direction);
        }

        switch (direction) {
            case GraphDirection.DOWN -> this.adjacentDown = node;
            case GraphDirection.UP -> this.adjacentUp = node;
            case GraphDirection.NORTH -> this.adjacentNorth = node;
            case GraphDirection.SOUTH -> this.adjacentSouth = node;
            case GraphDirection.WEST -> this.adjacentWest = node;
            case GraphDirection.EAST -> this.adjacentEast = node;
            default -> {
            }
        }
    }

    public int getAdjacentMask() {
        return this.adjacentMask;
    }

    public TranslucentData getTranslucentData() {
        return this.translucentData;
    }

    public void setTranslucentData(TranslucentData translucentData) {
        if (translucentData == null) {
            throw new IllegalArgumentException("new translucentData cannot be null");
        }

        this.translucentData = translucentData;
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        for (var job : this.runningJobs) {
            job.setCancelled();
        }
        this.runningJobs.clear();

        this.clearRenderState();
        this.disposed = true;
    }

    public int setInfo(@Nullable BuiltSectionInfo info) {
        if (info != null) {
            return this.setRenderState(info);
        } else {
            return this.clearRenderState();
        }
    }

    private int setRenderState(@NonNull BuiltSectionInfo info) {
        var prevFlags = this.region.getSectionFlags(this.sectionIndex);
        var prevVisibilityData = this.visibilityData;

        this.region.setSectionRenderState(this.sectionIndex, info);
        this.visibilityData = info.visibilityData;

        int changes = SectionInfoChange.NONE;

        // invalidate the graph if the connectivity of this section changes. Changes to the BE and sprite flags are indirectly detected by checking for changes to the data directly hereafter.
        if (prevFlags != this.region.getSectionFlags(this.sectionIndex) || prevVisibilityData != this.visibilityData) {
            changes |= SectionInfoChange.GRAPH;
        }

        // Render lists need to be invalidated when the lists of BEs or sprites change since they are baked into the render list itself, and thus simply re-rendering the same render list won't update the presentation as it works for the meshes.
        if (info.culledBlockEntities != null || info.animatedSprites != null) {
            changes |= SectionInfoChange.RENDER_LIST;
        }

        return changes;
    }

    private int clearRenderState() {
        var wasBuilt = this.isBuilt();

        this.region.clearSectionRenderState(this.sectionIndex);
        this.visibilityData = null;

        // Invalidate graph when a previously built section is removed. Invalidating render lists too here doesn't make any sense since the section would still be in the tree until the graph is re-traversed.
        return wasBuilt ? SectionInfoChange.GRAPH : SectionInfoChange.NONE;
    }

    public void setLastMeshResultSize(long size) {
        this.lastMeshResultSize = size;
    }

    public long getLastMeshResultSize() {
        return this.lastMeshResultSize;
    }

    /**
     * Returns the chunk section position which this render refers to in the level.
     */
    public SectionPos getPosition() {
        return SectionPos.of(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * @return The x-coordinate of the origin position of this chunk render
     */
    public int getOriginX() {
        return this.chunkX << 4;
    }

    /**
     * @return The y-coordinate of the origin position of this chunk render
     */
    public int getOriginY() {
        return this.chunkY << 4;
    }

    /**
     * @return The z-coordinate of the origin position of this chunk render
     */
    public int getOriginZ() {
        return this.chunkZ << 4;
    }

    /**
     * @return The squared distance from the center of this chunk to the given block position
     */
    public float getSquaredDistance(float x, float y, float z) {
        float xDist = x - this.getCenterX();
        float yDist = y - this.getCenterY();
        float zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    /**
     * @return The x-coordinate of the center position of this chunk render
     */
    public int getCenterX() {
        return this.getOriginX() + 8;
    }

    /**
     * @return The y-coordinate of the center position of this chunk render
     */
    public int getCenterY() {
        return this.getOriginY() + 8;
    }

    /**
     * @return The z-coordinate of the center position of this chunk render
     */
    public int getCenterZ() {
        return this.getOriginZ() + 8;
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public int getChunkZ() {
        return this.chunkZ;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public String toString() {
        return String.format("RenderSection at chunk (%d, %d, %d) from (%d, %d, %d) to (%d, %d, %d)",
                this.chunkX, this.chunkY, this.chunkZ,
                this.getOriginX(), this.getOriginY(), this.getOriginZ(),
                this.getOriginX() + 15, this.getOriginY() + 15, this.getOriginZ() + 15);
    }

    public boolean isBuilt() {
        return (this.region.getSectionFlags(this.sectionIndex) & RenderSectionFlags.MASK_IS_BUILT) != 0;
    }

    public int getSectionIndex() {
        return this.sectionIndex;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public boolean needsRender() {
        return this.region.sectionNeedsRender(this.sectionIndex);
    }

    public void resetOnFirstVisit(int token) {
        this.searchToken = token;
        this.incomingDirectionsWide = 0;
        this.incomingDirectionsRegular = 0;
        this.incomingDirectionsLocal = 0;
    }

    public int getSearchToken() {
        return this.searchToken;
    }

    public int getIncomingDirectionsWide() {
        return this.incomingDirectionsWide;
    }

    public int getIncomingDirectionsRegular() {
        return this.incomingDirectionsRegular;
    }

    public int getIncomingDirectionsLocal() {
        if (this.incomingDirectionsLocal == -1) {
            return 0;
        }

        return this.incomingDirectionsLocal;
    }

    public void addIncomingDirectionsWide(int directions) {
        this.incomingDirectionsWide |= directions;
    }

    public void addIncomingDirectionsRegular(int directions) {
        this.incomingDirectionsRegular |= directions;
    }

    public void addIncomingDirectionsLocal(int directions) {
        if (this.incomingDirectionsLocal == -1) {
            return;
        }

        this.incomingDirectionsLocal |= directions;
    }

    public void blockLocalIncoming() {
        this.incomingDirectionsLocal = -1;
    }

    private static final int ANGLE_BITS = 10;
    private static final int ANGLE_MASK = (1 << ANGLE_BITS) - 1;
    private static final long ANGLES_MIN_MASK =
            (long) ANGLE_MASK * (1 | (1L << (ANGLE_BITS * 2)) | (1L << (ANGLE_BITS * 4)));
    private static final long ANGLES_MAX_MASK =
            (long) ANGLE_MASK * ((1L << ANGLE_BITS) | (1L << (ANGLE_BITS * 3)) | (1L << (ANGLE_BITS * 5)));
    private static final int LUT_DIM = 32;
    private static final int LUT_SHIFT = 5; // (1 << 5) = 32

    /**
     * Lookup table for 20-bit packed (maxAngle(10)<<10) | minAngle(10).
     * Indexed by [rise + run * 32], where rise/run are integers in [0, 31].
     */
    private static final int[] ANGLE_LUT = new int[LUT_DIM * LUT_DIM];

    static {
        for (int run = 0; run < LUT_DIM; run++) {
            for (int rise = 0; rise < LUT_DIM; rise++) {
                ANGLE_LUT[rise + run * LUT_DIM] = generateAngles(rise, run);
            }
        }
    }

    private static int generateAngles(int rise, int run) {
        double minAngle = Math.atan2(rise - 1, run + 1);
        double maxAngle = Math.atan2(rise + 1, run - 1);

        // Quantize angles to 10-bit range [0, 1023]
        int minQuant = (int) (Math.max(0.0, minAngle) * (ANGLE_MASK / (Math.PI / 2.0)));
        int maxQuant = (int) (Math.min(Math.PI / 2.0, maxAngle) * (ANGLE_MASK / (Math.PI / 2.0)));

        return (minQuant & ANGLE_MASK) | ((maxQuant & ANGLE_MASK) << ANGLE_BITS);
    }

    public void setOriginAngles() {
        this.allowedAngles = ANGLES_MAX_MASK;
    }

    /**
     * Intersects the allowed angles from the 'other' section with the base angles
     * subtended by this section.
     *
     * @param origin The origin of the visibility check.
     * @param other  The parent/previous section from which visibility is being propagated.
     * @param token  The current BFS token (similar to frame count).
     * @return false if this section is guaranteed not visible, true otherwise.
     */
    public boolean intersectSlopes(SectionPos origin, RenderSection other, int token) {
        var dx = Math.abs(origin.getX() - this.getChunkX());
        var dy = Math.abs(origin.getY() - this.getChunkY());
        var dz = Math.abs(origin.getZ() - this.getChunkZ());

        // Shift to [0, 31] for LUT lookup
        while ((dx | dy | dz) >= 32) {
            // This is only true for the outermost rings of sections that have a distance
            // of 32 when 32 chunks are visible, so we don't use more complex
            // 32-Integer.numberOfLeadingZeros and per-plane shifting.
            dx >>= 1;
            dy >>= 1;
            dz >>= 1;
        }

        long baseAngles = ANGLE_LUT[dx + (dy << LUT_SHIFT)] |
                ((long) ANGLE_LUT[dz + (dx << LUT_SHIFT)] << (2 * ANGLE_BITS)) |
                ((long) ANGLE_LUT[dy + (dz << LUT_SHIFT)] << (4 * ANGLE_BITS));

        long pathAngles = parallel_unsigned_max_min(other.allowedAngles, baseAngles);

        // Check if max < min for any plane, which means the path is occluded.
        long borrows = parallel_unsigned_lt_msbs((pathAngles & ANGLES_MAX_MASK) >> ANGLE_BITS, pathAngles & ANGLES_MIN_MASK);
        if (borrows != 0) {
            return false;
        }

        if (this.searchToken == token) {
            // This section has been visited before in *this search*.
            // Union the angles: [min(oldMin, newMin), max(oldMax, newMax)]
            pathAngles = parallel_unsigned_min_max(pathAngles, this.allowedAngles);
        }
        this.allowedAngles = pathAngles;

        return true;
    }

    /**
     * Performs a parallel unsigned less-than comparison (a < b) for 6 10-bit lanes.
     *
     * @param a 6 packed 10-bit values
     * @param b 6 packed 10-bit values
     * @return A long with the MSB of each lane (bit 9, 19, 29, ...) set if a_k < b_k.
     * <p>
     * Based on `vhaddu8(~a, b)` (LTU_VARIANT 0) from:
     * <a href="https://stackoverflow.com/a/68717720/3694">Stackoverflow</a>
     * Citing Peter L. Montgomery's observation
     * <a href="https://groups.google.com/d/msg/comp.arch/gXFuGZtZKag/_5yrz2zDbe4J">comp.arch, 2000/02/11</a>:
     * (A+B)/2 = (A AND B) + (A XOR B)/2.
     * The MSB of (A+B)/2 is the same as the carry-out of (A+B),
     * and `vhaddu(~a, b)` calculates `(~a+b)/2`, which sets the MSB if `b > a`.
     */
    private static long parallel_unsigned_lt_msbs(long a, long b) {
        // MSB (sign bit) for each 10-bit lane
        final long LANE_MSB = 1L << (ANGLE_BITS - 1);
        final long LANE_MSB_MASK = (LANE_MSB << (ANGLE_BITS * 0)) |
                (LANE_MSB << (ANGLE_BITS * 1)) |
                (LANE_MSB << (ANGLE_BITS * 2)) |
                (LANE_MSB << (ANGLE_BITS * 3)) |
                (LANE_MSB << (ANGLE_BITS * 4)) |
                (LANE_MSB << (ANGLE_BITS * 5));
        // All bits *except* the MSB for each 10-bit lane
        final long LANE_NON_MSB_MASK = ((1L << (ANGLE_BITS * 6)) - 1) ^ LANE_MSB_MASK;

        long vhaddu_result = (~a & b) + (((~a ^ b) >>> 1) & LANE_NON_MSB_MASK);
        // Return just the MSBs, which are set if a_k < b_k
        return vhaddu_result & LANE_MSB_MASK;
    }

    /**
     * Creates a 30-bit mask (0x3FF per field) where a field is all 1s
     * if a_k < b_k, and 0 otherwise.
     */
    private static long parallel_unsigned_borrow_mask(long a, long b) {
        // 'msbs' has bits 9, 19, 29, ... set if a_k < b_k
        long msbs = parallel_unsigned_lt_msbs(a, b);

        // Implements sign_to_mask for 10-bit lanes.
        // (a + a - (a >> 9)) adapted from 8-bit (a + a - (a >> 7))
        // This expands the MSB of each lane to fill the entire lane (0x200 -> 0x3FF)
        return msbs + msbs - (msbs >>> 9);
    }

    /**
     * Performs 6 parallel 10-bit *unsigned* min/max operations.
     *
     * @param a 6 packed 10-bit values
     * @param b 6 packed 10-bit values
     * @return 6 packed 10-bit values containing min(a_0, b_0), max(a_1, b_1), min(a_2, b_2) ..
     */
    private static long parallel_unsigned_min_max(long a, long b) {
        long mask = parallel_unsigned_borrow_mask(a, b);  // all bits set where a < b
        mask ^= ANGLES_MAX_MASK;  // flip masks for max angles to make it a min operation
        return (a & mask) | (b & ~mask);  // select based on mask
    }

    /**
     * Performs 6 parallel 10-bit *unsigned* min/max operations.
     *
     * @param a 6 packed 10-bit values
     * @param b 6 packed 10-bit values
     * @return 6 packed 10-bit values containing max(a_0, b_0), min(a_1, b_1), max(a_2, b_2) ..
     */
    private static long parallel_unsigned_max_min(long a, long b) {
        long mask = parallel_unsigned_borrow_mask(a, b);  // all bits set where a < b
        mask ^= ANGLES_MIN_MASK;  // flip masks for min angles to make it a max operation
        return (a & mask) | (b & ~mask);  // select based on mask
    }

    /**
     * Returns the occlusion culling data which determines this chunk's connectedness on the visibility graph.
     */
    public long[] getVisibilityData() {
        return this.visibilityData;
    }

    public void clearRunningJob(ChunkJob job) {
        this.runningJobs.remove(job);
    }

    public void addRunningJob(ChunkJob job) {
        this.runningJobs.add(job);
    }

    public int getPendingUpdate() {
        return this.pendingUpdateType;
    }

    public long getPendingUpdateSince() {
        return this.pendingUpdateSince;
    }

    public void setPendingUpdate(int type, long now) {
        this.pendingUpdateType = type;
        this.pendingUpdateSince = now;
    }

    public void clearPendingUpdate() {
        this.pendingUpdateType = 0;
    }

    public void prepareTrigger(boolean isDirectTrigger) {
        if (this.translucentData != null) {
            this.translucentData.prepareTrigger(isDirectTrigger);
        }
    }

    public boolean addBuildOutput(BuilderTaskOutput output) {
        var hasNoPendingOutputs = this.pendingBuildOutput == null && this.pendingDynamicSortOutput == null;

        if (output instanceof ChunkBuildOutput buildOutput) {
            this.addMeshBuildOutput(buildOutput);
        } else if (output instanceof ChunkSortOutput sortOutput) {
            this.addDynamicSortOutput(sortOutput);
        } else {
            throw new IllegalArgumentException("Unexpected output type: " + output.getClass());
        }

        // signal that this section needs build processing only once
        return hasNoPendingOutputs && (this.pendingBuildOutput != null || this.pendingDynamicSortOutput != null);
    }

    private void addMeshBuildOutput(ChunkBuildOutput output) {
        // TODO: check that the sorting output actually matches the translucent data that's on the render section?
        // TODO: there's some sort of new flickering going on with sorting sometimes
        // records build output if it's newer than what is uploaded, or newer than what is pending to be uploaded
        if (this.pendingBuildOutput == null && output.submitTime > this.frameOfBuildSubmit ||
                this.pendingBuildOutput != null && output.submitTime > this.pendingBuildOutput.submitTime) {
            this.pendingBuildOutput = output;

            // if there's a dynamic sort submitted before the rebuild but the rebuild reuses the index data, then we need to accept the sort. If the rebuild doesn't reuse uploaded index data, then the previously scheduled sort will be invalid.
            if (output.containsNewIndexData()) {
                this.pendingDynamicSortOutput = output;
            }
        }
    }

    private void addDynamicSortOutput(ChunkSortOutput output) {
        if (this.pendingDynamicSortOutput == null && output.submitTime > this.frameOfSortSubmit ||
                this.pendingDynamicSortOutput != null && output.submitTime > this.pendingDynamicSortOutput.submitTime) {
            this.pendingDynamicSortOutput = output;
        }
    }

    public @Nullable ChunkBuildOutput retrievePendingBuildOutput() {
        ChunkBuildOutput output = null;
        if (this.pendingBuildOutput != null) {
            this.frameOfBuildSubmit = this.pendingBuildOutput.submitTime;
            output = this.pendingBuildOutput;
        }
        this.pendingBuildOutput = null;
        return output;
    }

    public @Nullable ChunkSortOutput retrievePendingDynamicSortOutput(ChunkBuildOutput buildOutput) {
        ChunkSortOutput output = null;
        if (this.pendingDynamicSortOutput != null) {
            this.frameOfSortSubmit = this.pendingDynamicSortOutput.submitTime;
            if (this.pendingDynamicSortOutput != buildOutput) {
                output = this.pendingDynamicSortOutput;
            }
        }
        this.pendingDynamicSortOutput = null;
        return output;
    }
}
