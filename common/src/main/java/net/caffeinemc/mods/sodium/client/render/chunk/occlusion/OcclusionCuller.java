package net.caffeinemc.mods.sodium.client.render.chunk.occlusion;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.util.collections.DoubleBufferedQueue;
import net.caffeinemc.mods.sodium.client.util.collections.ReadQueue;
import net.caffeinemc.mods.sodium.client.util.collections.WriteQueue;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/*
 * TODO idea: traverse octants of the world with separate threads for better performance?
 */
public class OcclusionCuller {
    private final Long2ReferenceMap<RenderSection> sections;
    private final Level level;
    private final DoubleBufferedQueue<RenderSection> queue = new DoubleBufferedQueue<>();

    private int outOfWorldRadius;
    private int outOfWorldHeight;
    private int outOfWorldDirection;

    private volatile int tokenSource = 0;

    private int token;
    private GraphOcclusionVisitor visitor;
    private Viewport viewport;
    private SectionPos origin;
    private SectionPos inBoundsOrigin;
    private float searchDistance;
    private boolean useOcclusionCulling;

    // The bounding box of a chunk section must be large enough to contain all possible geometry within it. Block models
    // can extend outside a block volume by +/- 1.0 blocks on all axis. Additionally, we make use of a small epsilon
    // to deal with floating point imprecision during a frustum check (see GH#2132).
    public static final float CHUNK_SECTION_RADIUS = 8.0f /* chunk bounds */;
    public static final float CHUNK_SECTION_MARGIN = 1.0f /* maximum model extent */ + 0.125f /* epsilon */;
    public static final float CHUNK_SECTION_PADDED_RADIUS = CHUNK_SECTION_RADIUS + CHUNK_SECTION_MARGIN;

    public interface GraphOcclusionVisitor {
        default boolean visitTestVisible(RenderSection section) {
            return true;
        }

        void visit(RenderSection section);

        default boolean isWithinFrustum(Viewport viewport, RenderSection section) {
            return viewport.isBoxVisible(section.getCenterX(), section.getCenterY(), section.getCenterZ());
        }

        default int getOutwardDirections(SectionPos origin, RenderSection section) {
            int planes = 0;

            planes |= section.getChunkX() <= origin.getX() ? 1 << GraphDirection.WEST : 0;
            planes |= section.getChunkX() >= origin.getX() ? 1 << GraphDirection.EAST : 0;

            planes |= section.getChunkY() <= origin.getY() ? 1 << GraphDirection.DOWN : 0;
            planes |= section.getChunkY() >= origin.getY() ? 1 << GraphDirection.UP : 0;

            planes |= section.getChunkZ() <= origin.getZ() ? 1 << GraphDirection.NORTH : 0;
            planes |= section.getChunkZ() >= origin.getZ() ? 1 << GraphDirection.SOUTH : 0;

            return planes;
        }

        long UP_DOWN_OCCLUDED = (1L << VisibilityEncoding.bit(GraphDirection.DOWN, GraphDirection.UP)) | (1L << VisibilityEncoding.bit(GraphDirection.UP, GraphDirection.DOWN));
        long NORTH_SOUTH_OCCLUDED = (1L << VisibilityEncoding.bit(GraphDirection.NORTH, GraphDirection.SOUTH)) | (1L << VisibilityEncoding.bit(GraphDirection.SOUTH, GraphDirection.NORTH));
        long WEST_EAST_OCCLUDED = (1L << VisibilityEncoding.bit(GraphDirection.WEST, GraphDirection.EAST)) | (1L << VisibilityEncoding.bit(GraphDirection.EAST, GraphDirection.WEST));

        default long getAngleVisibilityMask(Viewport viewport, RenderSection section) {
            var transform = viewport.getTransform();
            var dx = Math.abs(transform.x - section.getCenterX());
            var dy = Math.abs(transform.y - section.getCenterY());
            var dz = Math.abs(transform.z - section.getCenterZ());

            var angleOcclusionMask = 0L;
            if (dx > dy || dz > dy) {
                angleOcclusionMask |= UP_DOWN_OCCLUDED;
            }
            if (dx > dz || dy > dz) {
                angleOcclusionMask |= NORTH_SOUTH_OCCLUDED;
            }
            if (dy > dx || dz > dx) {
                angleOcclusionMask |= WEST_EAST_OCCLUDED;
            }

            return ~angleOcclusionMask;
        }

        default int getDirectionSets(Viewport viewport, RenderSection section) {
            var transform = viewport.getTransform();

            // determine which base perspectives need to be combined based on the camera position relative to the section.
            // these bitmasks correspond to the base directions in DirectionalVisGraph.DIRECTION_SETS
            int directionSetsX = 0;
            if (transform.x >= section.getOriginX()) {
                directionSetsX = 0b00001111;
            }
            if (transform.x <= section.getOriginX() + 16) {
                directionSetsX |= 0b11110000;
            }

            int directionSetsZ = 0;
            if (transform.z >= section.getOriginZ()) {
                directionSetsZ = 0b00110011;
            }
            if (transform.z <= section.getOriginZ() + 16) {
                directionSetsZ |= 0b11001100;
            }

            int directionSetsY = 0;
            if (transform.y >= section.getOriginY()) {
                directionSetsY = 0b01010101;
            }
            if (transform.y <= section.getOriginY() + 16) {
                directionSetsY |= 0b10101010;
            }

            return directionSetsX & directionSetsY & directionSetsZ;
        }
    }

    public OcclusionCuller(Long2ReferenceMap<RenderSection> sections, Level level) {
        this.sections = sections;
        this.level = level;
    }

    public int findVisible(GraphOcclusionVisitor visitor,
                           Viewport viewport,
                           float searchDistance,
                           boolean useOcclusionCulling,
                           CancellationToken cancellationToken) {
        this.visitor = visitor;
        this.viewport = viewport;
        this.searchDistance = searchDistance;
        this.useOcclusionCulling = useOcclusionCulling;

        this.queue.reset();

        // get a token for this bfs run by incrementing the counter.
        // It doesn't need to be atomic since there's no concurrent access, but it needs to be synced to other threads.
        this.token = this.tokenSource;
        this.tokenSource = this.token + 1;

        this.origin = viewport.getChunkCoord();
        this.inBoundsOrigin = this.origin;

        var initWriteQueue = this.queue.write();
        this.init(initWriteQueue);

        // initial write so that the first flip doesn't stop the loop
        if (this.outOfWorldRadius == 0) {
            while (initWriteQueue.isEmpty() && this.initOutsideWorldHeight(initWriteQueue)) {
                this.outOfWorldRadius++;
            }
        }

        if (this.getRenderSection(this.origin) == null) {
            // origin outside of world
            this.inBoundsOrigin = null;
        }

        while (this.queue.flip()) {
            if (cancellationToken.isCancelled()) {
                break;
            }

            if (this.outOfWorldRadius > 0) {
                this.initOutsideWorldHeight(this.queue.write());
                this.outOfWorldRadius++;
            }

            processQueue(this.queue.read(), this.queue.write());
        }

        this.addNearbySections(visitor, viewport);

        this.visitor = null;
        this.viewport = null;

        return this.token;
    }

    private void processQueue(ReadQueue<RenderSection> readQueue,
                              WriteQueue<RenderSection> writeQueue) {
        RenderSection section;
        var origin = this.viewport.getChunkCoord();

        // only visible sections are entered into the queue
        while ((section = readQueue.dequeue()) != null) {
            int connections;

            {
                if (this.useOcclusionCulling) {
                    var visibilityDataSet = section.getVisibilityData();
                    if (visibilityDataSet == null) {
                        // No visibility data, so we can't traverse into any neighbors.
                        continue;
                    }

                    // get the visibility data for the camera perspective relative to this section
                    var visibilityData = this.joinVisibilityData(visibilityDataSet, section, this.viewport);

                    // occlude paths through the section if it's being viewed at an angle where
                    // the other side can't possibly be seen
                    visibilityData &= this.visitor.getAngleVisibilityMask(this.viewport, section);

                    // When using occlusion culling, we can only traverse into neighbors for which there is a path of
                    // visibility through this chunk. This is determined by taking all the incoming paths to this chunk and
                    // creating a union of the outgoing paths from those.
                    connections = VisibilityEncoding.getConnections(visibilityData, section.getIncomingDirections());
                } else {
                    // Not using any occlusion culling, so traversing in any direction is legal.
                    connections = GraphDirectionSet.ALL;
                }

                // We can only traverse *outwards* from the center of the graph search, so mask off any invalid
                // directions.
                connections &= this.visitor.getOutwardDirections(origin, section);
            }

            visitNeighbors(writeQueue, section, connections, this.inBoundsOrigin);
        }
    }

    private long joinVisibilityData(long[] visibilityDataSet, RenderSection section, Viewport viewport) {
        if (visibilityDataSet.length == 1) {
            return visibilityDataSet[0];
        }

        int directionSets = this.visitor.getDirectionSets(viewport, section);

        // Combine the relevant visibility data sets.
        // Since each perspective can be seen from two opposite sides, two bits in each mask are set.
        long visibilityData = 0L;
        if ((directionSets & 0b10000001) != 0) {
            visibilityData |= visibilityDataSet[0];
        }
        if ((directionSets & 0b01000010) != 0) {
            visibilityData |= visibilityDataSet[1];
        }
        if ((directionSets & 0b00100100) != 0) {
            visibilityData |= visibilityDataSet[2];
        }
        if ((directionSets & 0b00011000) != 0) {
            visibilityData |= visibilityDataSet[3];
        }

        return visibilityData;
    }

    private static boolean isWithinRenderDistance(CameraTransform camera, RenderSection section, float maxDistance) {
        // origin point of the chunk's bounding box (in view space)
        int ox = section.getOriginX() - camera.intX;
        int oy = section.getOriginY() - camera.intY;
        int oz = section.getOriginZ() - camera.intZ;

        // coordinates of the point to compare (in view space)
        // this is the closest point within the bounding box to the center (0, 0, 0)
        float dx = nearestToZero(ox - 1, ox + 17) - camera.fracX;
        float dy = nearestToZero(oy - 1, oy + 17) - camera.fracY;
        float dz = nearestToZero(oz - 1, oz + 17) - camera.fracZ;

        // vanilla's "cylindrical fog" algorithm
        // max(length(distance.xz), abs(distance.y))
        return (((dx * dx) + (dz * dz)) < (maxDistance * maxDistance)) && (Math.abs(dy) < maxDistance);
    }

    private void visitNeighbors(WriteQueue<RenderSection> queue, RenderSection section, int outgoing, SectionPos origin) {
        // Only traverse into neighbors which are actually present.
        // This avoids a null-check on each invocation to enqueue, and since the compiler will see that a null
        // is never encountered (after profiling), it will optimize it away.
        outgoing &= section.getAdjacentMask();

        // Check if there are any valid connections left, and if not, early-exit.
        if (outgoing == GraphDirectionSet.NONE) {
            return;
        }

        if (origin == null) {
            // the viewpoint is outside the world, so the angle computations relying on propagating angle information
            // from the origin section to the others won't work.
            if (GraphDirectionSet.contains(outgoing, GraphDirection.DOWN)) {
                visitNode(queue, section.adjacentDown, GraphDirectionSet.of(GraphDirection.UP));
            }

            if (GraphDirectionSet.contains(outgoing, GraphDirection.UP)) {
                visitNode(queue, section.adjacentUp, GraphDirectionSet.of(GraphDirection.DOWN));
            }

            if (GraphDirectionSet.contains(outgoing, GraphDirection.NORTH)) {
                visitNode(queue, section.adjacentNorth, GraphDirectionSet.of(GraphDirection.SOUTH));
            }

            if (GraphDirectionSet.contains(outgoing, GraphDirection.SOUTH)) {
                visitNode(queue, section.adjacentSouth, GraphDirectionSet.of(GraphDirection.NORTH));
            }

            if (GraphDirectionSet.contains(outgoing, GraphDirection.WEST)) {
                visitNode(queue, section.adjacentWest, GraphDirectionSet.of(GraphDirection.EAST));
            }

            if (GraphDirectionSet.contains(outgoing, GraphDirection.EAST)) {
                visitNode(queue, section.adjacentEast, GraphDirectionSet.of(GraphDirection.WEST));
            }
            return;
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.DOWN) &&
                section.adjacentDown.intersectSlopes(this.inBoundsOrigin, section, this.token)) {
            visitNode(queue, section.adjacentDown, GraphDirectionSet.of(GraphDirection.UP));
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.UP) &&
                section.adjacentUp.intersectSlopes(this.inBoundsOrigin, section, this.token)) {
            visitNode(queue, section.adjacentUp, GraphDirectionSet.of(GraphDirection.DOWN));
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.NORTH) &&
                section.adjacentNorth.intersectSlopes(this.inBoundsOrigin, section, this.token)) {
            visitNode(queue, section.adjacentNorth, GraphDirectionSet.of(GraphDirection.SOUTH));
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.SOUTH) &&
                section.adjacentSouth.intersectSlopes(this.inBoundsOrigin, section, this.token)) {
            visitNode(queue, section.adjacentSouth, GraphDirectionSet.of(GraphDirection.NORTH));
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.WEST) &&
                section.adjacentWest.intersectSlopes(this.inBoundsOrigin, section, this.token)) {
            visitNode(queue, section.adjacentWest, GraphDirectionSet.of(GraphDirection.EAST));
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.EAST) &&
                section.adjacentEast.intersectSlopes(this.inBoundsOrigin, section, this.token)) {
            visitNode(queue, section.adjacentEast, GraphDirectionSet.of(GraphDirection.WEST));
        }
    }

    private void visitNode(WriteQueue<RenderSection> queue, RenderSection section, int incoming) {
        // isn't usually null, but can be null if the bfs is happening during loading or unloading of chunks
        if (section == null) {
            return;
        }

        if (section.getLastVisibleSearchToken() != this.token) {
            // This is the first time we are visiting this section during the given token, so we must
            // reset the state.
            section.setLastVisibleSearchToken(this.token);
            section.setIncomingDirections(GraphDirectionSet.NONE);

            if (isWithinRenderDistance(this.viewport.getTransform(), section, this.searchDistance) &&
                    this.visitor.isWithinFrustum(this.viewport, section) &&
                    this.visitor.visitTestVisible(section)) {
                this.visitor.visit(section);
                queue.enqueue(section);
            }
        }

        section.addIncomingDirections(incoming);
    }

    @SuppressWarnings("ManualMinMaxCalculation") // we know what we are doing.
    private static int nearestToZero(int min, int max) {
        // this compiles to slightly better code than Math.min(Math.max(0, min), max)
        int clamped = 0;
        if (min > 0) {
            clamped = min;
        }
        if (max < 0) {
            clamped = max;
        }
        return clamped;
    }

    public static boolean isWithinNearbySectionFrustum(Viewport viewport, RenderSection section) {
        return viewport.isBoxVisibleLooser(section.getCenterX(), section.getCenterY(), section.getCenterZ());
    }

    // This method visits sections near the origin that are not in the path of the graph traversal
    // but have bounding boxes that may intersect with the frustum. It does this additional check
    // for all neighboring, even diagonally neighboring, sections around the origin to render them
    // if their extended bounding box is visible, and they may render large models that extend
    // outside the 16x16x16 base volume of the section.
    private void addNearbySections(GraphOcclusionVisitor visitor, Viewport viewport) {
        var origin = viewport.getChunkCoord();
        var originX = origin.getX();
        var originY = origin.getY();
        var originZ = origin.getZ();

        for (var dx = -1; dx <= 1; dx++) {
            for (var dy = -1; dy <= 1; dy++) {
                for (var dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    var section = this.getRenderSection(originX + dx, originY + dy, originZ + dz);

                    // additionally render not yet visited but visible sections
                    if (section != null && section.getLastVisibleSearchToken() != this.token && isWithinNearbySectionFrustum(viewport, section)) {
                        // reset state on first visit, but don't enqueue
                        section.setLastVisibleSearchToken(this.token);

                        visitor.visit(section);
                    }
                }
            }
        }
    }

    private void init(WriteQueue<RenderSection> queue) {
        var origin = this.viewport.getChunkCoord();

        if (origin.getY() < this.level.getMinSectionY()) {
            // below the level
            this.outOfWorldRadius = 0;
            this.outOfWorldHeight = this.level.getMinSectionY();
            this.outOfWorldDirection = GraphDirection.DOWN;
        } else if (origin.getY() > this.level.getMaxSectionY()) {
            // above the level
            this.outOfWorldRadius = 0;
            this.outOfWorldHeight = this.level.getMaxSectionY();
            this.outOfWorldDirection = GraphDirection.UP;
        } else {
            this.outOfWorldRadius = -1;
            this.initWithinWorld(queue);
        }
    }

    private void initWithinWorld(WriteQueue<RenderSection> queue) {
        var origin = this.viewport.getChunkCoord();
        var section = this.getRenderSection(origin.getX(), origin.getY(), origin.getZ());

        if (section == null) {
            return;
        }

        section.setOriginAngles();
        section.setLastVisibleSearchToken(this.token);
        section.setIncomingDirections(GraphDirectionSet.NONE);

        this.visitor.visit(section);

        int outgoing;

        if (this.useOcclusionCulling) {
            // Since the camera is located inside this chunk, there are no "incoming" directions. So we need to instead
            // find any possible paths out of this chunk and enqueue those neighbors.
            var visibilityDataSet = section.getVisibilityData();
            if (visibilityDataSet == null) {
                // No visibility data, so we can't traverse into any neighbors.
                return;
            }

            outgoing = VisibilityEncoding.getConnections(
                    this.joinVisibilityData(visibilityDataSet, section, this.viewport));
        } else {
            // Occlusion culling is disabled, so we can traverse into any neighbor.
            outgoing = GraphDirectionSet.ALL;
        }

        visitNeighbors(queue, section, outgoing, this.origin);
    }

    // Enqueues sections that are inside the viewport using diamond spiral iteration to avoid sorting and ensure a
    // consistent order. Innermost layers are enqueued first. Within each layer, iteration starts at the northernmost
    // section and proceeds counterclockwise (N->W->S->E).
    private boolean initOutsideWorldHeight(WriteQueue<RenderSection> queue) {
        var radius = Mth.floor(this.searchDistance / 16.0f);
        var height = this.outOfWorldHeight;
        var direction = this.outOfWorldDirection;
        var layer = this.outOfWorldRadius;
        int originX = this.origin.getX();
        int originZ = this.origin.getZ();

        // Layer 0
        if (layer == 0) {
            this.tryInitNode(queue, originX, height, originZ, direction);
        }

        // Complete layers, excluding layer 0
        else if (layer <= radius) {
            for (int z = -layer; z < layer; z++) {
                int x = Math.abs(z) - layer;
                this.tryInitNode(queue, originX + x, height, originZ + z, direction);
            }

            for (int z = layer; z > -layer; z--) {
                int x = layer - Math.abs(z);
                this.tryInitNode(queue, originX + x, height, originZ + z, direction);
            }
        }

        // Incomplete layers
        else if (layer <= 2 * radius) {
            int l = layer - radius;

            for (int z = -radius; z <= -l; z++) {
                int x = -z - layer;
                this.tryInitNode(queue, originX + x, height, originZ + z, direction);
            }

            for (int z = l; z <= radius; z++) {
                int x = z - layer;
                this.tryInitNode(queue, originX + x, height, originZ + z, direction);
            }

            for (int z = radius; z >= l; z--) {
                int x = layer - z;
                this.tryInitNode(queue, originX + x, height, originZ + z, direction);
            }

            for (int z = -l; z >= -radius; z--) {
                int x = layer + z;
                this.tryInitNode(queue, originX + x, height, originZ + z, direction);
            }
        }

        // nothing more to init
        else {
            return false;
        }
        return true;
    }

    private void tryInitNode(WriteQueue<RenderSection> queue, int x, int y, int z, int direction) {
        var section = this.getRenderSection(x, y, z);

        visitNode(queue, section, GraphDirectionSet.of(direction));
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(SectionPos.asLong(x, y, z));
    }

    private RenderSection getRenderSection(SectionPos pos) {
        return this.sections.get(pos.asLong());
    }
}
