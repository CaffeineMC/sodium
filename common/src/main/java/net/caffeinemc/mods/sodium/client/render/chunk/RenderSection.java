package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.MeshResultSize;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJob;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.GraphDirection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.GraphDirectionSet;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.VisibilityEncoding;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private long visibilityData = VisibilityEncoding.NULL;

    private int incomingDirections;
    private int lastVisibleFrame = -1;

    private long allowedAngles;

    private int adjacentMask;
    public RenderSection
            adjacentDown,
            adjacentUp,
            adjacentNorth,
            adjacentSouth,
            adjacentWest,
            adjacentEast;

    // Rendering State
    private boolean built = false; // merge with the flags?
    private int flags = RenderSectionFlags.NONE;
    private BlockEntity @Nullable[] globalBlockEntities;
    private BlockEntity @Nullable[] culledBlockEntities;
    private TextureAtlasSprite @Nullable[] animatedSprites;
    @Nullable
    private TranslucentData translucentData;

    // Pending Update State
    @Nullable
    private ChunkJob runningJob = null;
    private long lastMeshResultSize = MeshResultSize.NO_DATA;

    private int pendingUpdateType;
    private long pendingUpdateSince;

    private int lastUploadFrame = -1;
    private int lastSubmittedFrame = -1;

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
            default -> { }
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
        if (this.runningJob != null) {
            this.runningJob.setCancelled();
            this.runningJob = null;
        }

        this.clearRenderState();
        this.disposed = true;
    }

    public boolean setInfo(@Nullable BuiltSectionInfo info) {
        if (info != null) {
            return this.setRenderState(info);
        } else {
            return this.clearRenderState();
        }
    }

    private boolean setRenderState(@NotNull BuiltSectionInfo info) {
        var prevBuilt = this.built;
        var prevFlags = this.flags;
        var prevVisibilityData = this.visibilityData;

        this.built = true;
        this.flags = info.flags;
        this.visibilityData = info.visibilityData;

        this.globalBlockEntities = info.globalBlockEntities;
        this.culledBlockEntities = info.culledBlockEntities;
        this.animatedSprites = info.animatedSprites;

        // the section is marked as having received graph-relevant changes if it's build state, flags, or connectedness has changed.
        // the entities and sprites don't need to be checked since whether they exist is encoded in the flags.
        return !prevBuilt || prevFlags != this.flags || prevVisibilityData != this.visibilityData;
    }

    private boolean clearRenderState() {
        var wasBuilt = this.built;

        this.built = false;
        this.flags = RenderSectionFlags.NONE;
        this.visibilityData = VisibilityEncoding.NULL;
        this.globalBlockEntities = null;
        this.culledBlockEntities = null;
        this.animatedSprites = null;

        // changes to data if it moves from built to not built don't matter, so only build state changes matter
        return wasBuilt;
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
     * @return The squared distance from the center of this chunk in the level to the center of the block position
     * given by {@param pos}
     */
    public float getSquaredDistance(BlockPos pos) {
        return this.getSquaredDistance(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
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
        return this.built;
    }

    public int getSectionIndex() {
        return this.sectionIndex;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }

    public int getIncomingDirections() {
        return this.incomingDirections;
    }

    public void addIncomingDirections(int directions) {
        this.incomingDirections |= directions;
    }

    public void setIncomingDirections(int directions) {
        this.incomingDirections = directions;
    }

    private static final int BITS_PER_PLANE = 21;
    private static final long PLANE_MASK = (1L << BITS_PER_PLANE) - 1L; // 0x1FFFFFL

    public void setOriginSlopes() {
        this.allowedAngles = -1L;
    }

    /**
     * Precomputed Lookup Table for base angle bitsets.
     * This is a 32x32 LUT, indexed by [run + (rise << 5)].
     */
    private static final int LUT_DIM_BITS = 5; // 2^5 = 32
    private static final int LUT_SIZE = 1 << (LUT_DIM_BITS * 2); // 32*32 = 1024
    private static final int LUT_MAX_IDX = (1 << LUT_DIM_BITS) - 1; // 31
    private static final int[] ANGLE_BITSET_LUT = new int[LUT_SIZE];

    /**
     * Calculates the 21-bit angle bitset for a given rise/run.
     * This converts the min/max slope cone into a bitset.
     *
     * @param rise The rise (dy)
     * @param run  The run (dx)
     * @return A 21-bit integer bitset.
     */
    private static int generateAngleBits(int rise, int run) {
        int minRise = rise - 1;
        int minRun = run + 1;
        int maxRise = rise + 1;
        int maxRun = run - 1;

        // Convert packed slopes to angles (in radians, 0 to PI/2)
        double minAngle = Math.atan2(minRise, minRun);
        double maxAngle = Math.atan2(maxRise, maxRun);

        final double ANGLE_PER_BIT = (Math.PI / 2.0) / BITS_PER_PLANE; // 90 degrees / 21 bits in radians

        int bits = 0;
        for (int i = 0; i < BITS_PER_PLANE; i++) {
            double bitStartAngle = i * ANGLE_PER_BIT;
            double bitEndAngle = (i + 1) * ANGLE_PER_BIT;
            if (bitEndAngle > minAngle && bitStartAngle < maxAngle) {
                bits |= 1 << i;
            }
        }
        return bits;
    }


    static {
        for (int i = 0; i < LUT_SIZE; i++) {
            int run = i & LUT_MAX_IDX;
            int rise = (i >> LUT_DIM_BITS);
            ANGLE_BITSET_LUT[i] = generateAngleBits(rise, run);
        }
    }

    /**
     * Intersects the allowed angles from the 'other' section with the base angles
     * subtended by this section.
     *
     * @param origin The origin of the visibility check.
     * @param other  The parent/previous section from which visibility is being propagated.
     * @param frame  The current frame number.
     * @return false if this section is guaranteed not visible, true otherwise.
     */
    public boolean intersectSlopes(SectionPos origin, RenderSection other, int frame) {
        var dx = Math.abs(origin.getX() - this.getChunkX());
        var dy = Math.abs(origin.getY() - this.getChunkY());
        var dz = Math.abs(origin.getZ() - this.getChunkZ());

        while ((dx|dy|dz) >= 32) {
            // This is only true for the outermost edge of sections that have a distance
            // of 32, so we don't use more complex 32-Integer.numberOfLeadingZeros and per-plane
            // shifting.
            dx >>= 1; dy >>= 1; dz >>= 1;
        }

        long baseAngles = ANGLE_BITSET_LUT[dx + (dy << LUT_DIM_BITS)] |
                ((long)ANGLE_BITSET_LUT[dx + (dz << LUT_DIM_BITS)] << BITS_PER_PLANE) |
                ((long)ANGLE_BITSET_LUT[dy + (dz << LUT_DIM_BITS)] << (BITS_PER_PLANE * 2));

        long pathAngles = baseAngles & other.allowedAngles;

        // If the intersection is empty for *any* plane, this path is occluded.
        if (anyPlaneHasEmptyBitset(pathAngles)) {
            return false;
        }

        if (this.lastVisibleFrame == frame) {
            // This section has been visited before *this frame* from another path.
            // The new allowed angles are the *union* of the old paths and this new path.
            pathAngles |= this.allowedAngles;
        }
        this.allowedAngles = pathAngles;

        return true;
    }

    private static boolean anyPlaneHasEmptyBitset(long angles) {
        final long SUB_MASK = 1L | (1L << BITS_PER_PLANE) | (1L << (BITS_PER_PLANE * 2));
        final long MSB_MASK = (1L << (BITS_PER_PLANE - 1)) | (1L << (BITS_PER_PLANE * 2 - 1)) | (1L << (BITS_PER_PLANE * 3 - 1));
        long borrows = (angles - SUB_MASK) & ~angles;
        return (borrows & MSB_MASK) != 0;
    }

    /**
     * Returns a bitfield containing the {@link RenderSectionFlags} for this built section.
     */
    public int getFlags() {
        return this.flags;
    }

    /**
     * Returns the occlusion culling data which determines this chunk's connectedness on the visibility graph.
     */
    public long getVisibilityData() {
        return this.visibilityData;
    }

    /**
     * Returns the collection of animated sprites contained by this rendered chunk section.
     */
    public TextureAtlasSprite @Nullable[] getAnimatedSprites() {
        return this.animatedSprites;
    }

    /**
     * Returns the collection of block entities contained by this rendered chunk.
     */
    public BlockEntity @Nullable[] getCulledBlockEntities() {
        return this.culledBlockEntities;
    }

    /**
     * Returns the collection of block entities contained by this rendered chunk, which are not part of its culling
     * volume. These entities should always be rendered regardless of the render being visible in the frustum.
     */
    public BlockEntity @Nullable[] getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

    public @Nullable ChunkJob getRunningJob() {
        return this.runningJob;
    }

    public void setRunningJob(@Nullable ChunkJob token) {
        this.runningJob = token;
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

    public int getLastUploadFrame() {
        return this.lastUploadFrame;
    }

    public void setLastUploadFrame(int lastSortFrame) {
        this.lastUploadFrame = lastSortFrame;
    }

    public int getLastSubmittedFrame() {
        return this.lastSubmittedFrame;
    }

    public void setLastSubmittedFrame(int lastSubmittedFrame) {
        this.lastSubmittedFrame = lastSubmittedFrame;
    }
}
