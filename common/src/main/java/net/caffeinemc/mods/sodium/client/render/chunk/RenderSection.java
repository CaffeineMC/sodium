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
import org.joml.Vector3f;

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

    private int adjacentMask;
    public RenderSection
            adjacentDown,
            adjacentUp,
            adjacentNorth,
            adjacentSouth,
            adjacentWest,
            adjacentEast;

    private final static boolean DISABLE_SLOPES = false;
    private long curMinSlopes;
    private long curMaxSlopes;

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

    private static final int SLOPE_ZERO = 1 << 8;
    private static final int SLOPE_INFINITY = 1;

    private static int packSlope(int rise, int run) {
        if (run <= 0) {
            return SLOPE_INFINITY;
        }
        return ((run & 0xFF) << 8) | (Math.max(0, rise) & 0xFF);
    }

    private static boolean isSlopeLess(int slope1, int slope2) {
        int x1 = slope1 >> 8;
        int y1 = slope1 & 0xFF;
        int x2 = slope2 >> 8;
        int y2 = slope2 & 0xFF;

        if (x1 == 0) return false;
        if (x2 == 0) return true;

        // this algebraic rearrangement avoids a division
        return y1 * x2 < y2 * x1;
    }

    private static int slopeMax(int slope1, int slope2) {
        return isSlopeLess(slope1, slope2) ? slope2 : slope1;
    }

    private static int slopeMin(int slope1, int slope2) {
        return isSlopeLess(slope1, slope2) ? slope1 : slope2;
    }

    private static long packAllSlopes(int slopeXY, int slopeXZ, int slopeYZ) {
        return (slopeXY & 0xFFFFL) |
                ((slopeXZ & 0xFFFFL) << 16) |
                ((slopeYZ & 0xFFFFL) << 32);
    }

    private static int getSlopeXY(long packedSlopes) {
        return (int)(packedSlopes & 0xFFFFL);
    }

    private static int getSlopeXZ(long packedSlopes) {
        return (int)((packedSlopes >> 16) & 0xFFFFL);
    }

    private static int getSlopeYZ(long packedSlopes) {
        return (int)((packedSlopes >> 32) & 0xFFFFL);
    }

    private static final long ALL_SLOPES_ZERO = packAllSlopes(SLOPE_ZERO, SLOPE_ZERO, SLOPE_ZERO);
    private static final long ALL_SLOPES_INFINITY = packAllSlopes(SLOPE_INFINITY, SLOPE_INFINITY, SLOPE_INFINITY);

    public boolean intersectSlopes(SectionPos origin, RenderSection other, int frame) {
        // Slope refinement tracking is based on the idea that by passing through a given cell,
        // the minimum and maximum angle of any cell that can be visited afterward is constrained.
        // A 2D visualization of this is here: https://mod.ifies.com/f/251025_raycast_vis_v4.html
        // We perform the same thing across the XY, XZ, and YZ planes separately to avoid the more
        // complex 3D frustum tracking math. It misses some things that could be pruned, but is quite fast.
        if (DISABLE_SLOPES)
            return true;

        if (this.lastVisibleFrame != frame) {
            this.curMinSlopes = ALL_SLOPES_INFINITY;
            this.curMaxSlopes = ALL_SLOPES_ZERO;
        }

        var dx = Math.abs(origin.x() - this.getChunkX());
        var dy = Math.abs(origin.y() - this.getChunkY());
        var dz = Math.abs(origin.z() - this.getChunkZ());

        int thisBaseMinXY = packSlope(dy - 1, dx + 1);
        int thisBaseMinXZ = packSlope(dz - 1, dx + 1);
        int thisBaseMinYZ = packSlope(dz - 1, dy + 1);

        int thisBaseMaxXY = packSlope(dy + 1, dx - 1);
        int thisBaseMaxXZ = packSlope(dz + 1, dx - 1);
        int thisBaseMaxYZ = packSlope(dz + 1, dy - 1);

        int otherMinXY = getSlopeXY(other.curMinSlopes);
        int otherMinXZ = getSlopeXZ(other.curMinSlopes);
        int otherMinYZ = getSlopeYZ(other.curMinSlopes);

        int otherMaxXY = getSlopeXY(other.curMaxSlopes);
        int otherMaxXZ = getSlopeXZ(other.curMaxSlopes);
        int otherMaxYZ = getSlopeYZ(other.curMaxSlopes);

        int propMinXY = slopeMax(otherMinXY, thisBaseMinXY);
        int propMaxXY = slopeMin(otherMaxXY, thisBaseMaxXY);
        if (!isSlopeLess(propMinXY, propMaxXY)) {
            return false;
        }

        int propMinXZ = slopeMax(otherMinXZ, thisBaseMinXZ);
        int propMaxXZ = slopeMin(otherMaxXZ, thisBaseMaxXZ);
        if (!isSlopeLess(propMinXZ, propMaxXZ)) {
            return false;
        }

        int propMinYZ = slopeMax(otherMinYZ, thisBaseMinYZ);
        int propMaxYZ = slopeMin(otherMaxYZ, thisBaseMaxYZ);
        if (!isSlopeLess(propMinYZ, propMaxYZ)) {
            return false;
        }

        int newMinXY = slopeMin(getSlopeXY(this.curMinSlopes), propMinXY);
        int newMinXZ = slopeMin(getSlopeXZ(this.curMinSlopes), propMinXZ);
        int newMinYZ = slopeMin(getSlopeYZ(this.curMinSlopes), propMinYZ);

        int newMaxXY = slopeMax(getSlopeXY(this.curMaxSlopes), propMaxXY);
        int newMaxXZ = slopeMax(getSlopeXZ(this.curMaxSlopes), propMaxXZ);
        int newMaxYZ = slopeMax(getSlopeYZ(this.curMaxSlopes), propMaxYZ);

        this.curMinSlopes = packAllSlopes(newMinXY, newMinXZ, newMinYZ);
        this.curMaxSlopes = packAllSlopes(newMaxXY, newMaxXZ, newMaxYZ);

        return true;
    }

    public void setOriginSlopes() {
        this.curMinSlopes = ALL_SLOPES_ZERO;
        this.curMaxSlopes = ALL_SLOPES_INFINITY;
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
