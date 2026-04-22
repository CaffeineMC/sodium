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
 *
 * Frustum visible implies regular visible implies wide visible.
 * Not wide visible implies not regular visible implies not frustum visible.
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
    private GraphOcclusionVisitor visitorWide;
    private GraphOcclusionVisitor visitorRegular;
    private VisibilityTestingVisitor visitorLocal;
    private Viewport viewport;
    private SectionPos origin;
    private SectionPos inBoundsOrigin;
    private float searchDistanceRegular;
    private float searchDistanceLocal;
    private boolean useOcclusionCulling;

    public interface GraphOcclusionVisitor {
        void visit(RenderSection visit, boolean inFrustum);
    }

    public interface VisibilityTestingVisitor extends GraphOcclusionVisitor {
        boolean visitTestVisible(RenderSection section);
    }

    private static boolean isWithinFrustum(Viewport viewport, RenderSection section) {
        return viewport.isBoxVisible(section.getCenterX(), section.getCenterY(), section.getCenterZ());
    }

    private static final long UP_DOWN_OCCLUDED = (1L << VisibilityEncoding.bit(GraphDirection.DOWN, GraphDirection.UP)) | (1L << VisibilityEncoding.bit(GraphDirection.UP, GraphDirection.DOWN));
    private static final long NORTH_SOUTH_OCCLUDED = (1L << VisibilityEncoding.bit(GraphDirection.NORTH, GraphDirection.SOUTH)) | (1L << VisibilityEncoding.bit(GraphDirection.SOUTH, GraphDirection.NORTH));
    private static final long WEST_EAST_OCCLUDED = (1L << VisibilityEncoding.bit(GraphDirection.WEST, GraphDirection.EAST)) | (1L << VisibilityEncoding.bit(GraphDirection.EAST, GraphDirection.WEST));

    private static long getAngleVisibilityMaskLocal(Viewport viewport, RenderSection section) {
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

    // width is 1 for regular (anywhere in current chunk) and 2 for wide (anywhere in neighboring chunks)
    private static long getAngleVisibilityMaskWide(Viewport viewport, RenderSection section, int width) {
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

    private static int getDirectionSetsLocal(Viewport viewport, RenderSection section) {
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

    // width is 1 for regular (anywhere in current chunk) and 2 for wide (anywhere in neighboring chunks)
    private static int getDirectionSetsWide(Viewport viewport, RenderSection section, int width) {
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

    private static int getOutwardDirectionsRegular(SectionPos origin, RenderSection section) {
        int planes = 0;

        planes |= section.getChunkX() <= origin.getX() ? 1 << GraphDirection.WEST : 0;
        planes |= section.getChunkX() >= origin.getX() ? 1 << GraphDirection.EAST : 0;

        planes |= section.getChunkY() <= origin.getY() ? 1 << GraphDirection.DOWN : 0;
        planes |= section.getChunkY() >= origin.getY() ? 1 << GraphDirection.UP : 0;

        planes |= section.getChunkZ() <= origin.getZ() ? 1 << GraphDirection.NORTH : 0;
        planes |= section.getChunkZ() >= origin.getZ() ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }

    private static int getOutwardDirectionsWide(SectionPos origin, RenderSection section) {
        int planes = 0;

        planes |= section.getChunkX() <= origin.getX() + 1 ? 1 << GraphDirection.WEST : 0;
        planes |= section.getChunkX() >= origin.getX() - 1 ? 1 << GraphDirection.EAST : 0;

        planes |= section.getChunkY() <= origin.getY() + 1 ? 1 << GraphDirection.DOWN : 0;
        planes |= section.getChunkY() >= origin.getY() - 1 ? 1 << GraphDirection.UP : 0;

        planes |= section.getChunkZ() <= origin.getZ() + 1 ? 1 << GraphDirection.NORTH : 0;
        planes |= section.getChunkZ() >= origin.getZ() - 1 ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }


    public OcclusionCuller(Long2ReferenceMap<RenderSection> sections, Level level) {
        this.sections = sections;
        this.level = level;
    }

    public void findVisible(GraphOcclusionVisitor visitorWide,
                            GraphOcclusionVisitor visitorRegular,
                            VisibilityTestingVisitor visitorLocal,
                            Viewport viewport,
                            float searchDistanceRegular,
                            float searchDistanceLocal,
                            boolean useOcclusionCulling,
                            CancellationToken cancellationToken) {
        this.visitorWide = visitorWide;
        this.visitorRegular = visitorRegular;
        this.visitorLocal = visitorLocal;

        this.viewport = viewport;
        this.searchDistanceRegular = searchDistanceRegular;
        this.searchDistanceLocal = searchDistanceLocal;
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

        this.addNearbySections(viewport);

        this.visitorWide = null;
        this.visitorRegular = null;
        this.visitorLocal = null;
        this.viewport = null;
    }

    private void processQueue(ReadQueue<RenderSection> readQueue,
                              WriteQueue<RenderSection> writeQueue) {
        RenderSection section;
        var origin = this.viewport.getChunkCoord();

        // only visible sections are entered into the queue
        while ((section = readQueue.dequeue()) != null) {
            var incomingDirectionsWide = section.getIncomingDirectionsWide();
            var incomingDirectionsRegular = section.getIncomingDirectionsRegular();
            var incomingDirectionsLocal = section.getIncomingDirectionsLocal();

            // visit queued sections here instead of before they are queued to get correct path information as visibility of different types may be added to a section at different times.
            // TODO: also do ray test on regular path?
            boolean wasInFrustum = false;
            if (incomingDirectionsRegular != 0) {
                if (incomingDirectionsLocal != 0) {
                    wasInFrustum = this.visitorLocal.visitTestVisible(section);
                }
                this.visitorRegular.visit(section, wasInFrustum);
            }
            this.visitorWide.visit(section, wasInFrustum);

            int outgoingWide, outgoingRegular, outgoingLocal;

            if (this.useOcclusionCulling) {
                var visibilityDataSet = section.getVisibilityData();
                if (visibilityDataSet == null) {
                    // No visibility data, so we can't traverse into any neighbors.
                    continue;
                }

                // get the visibility data for the camera perspective relative to this section
                var visibilityDataWide = this.joinVisibilityData(visibilityDataSet, section, this.viewport, 2);
                var visibilityDataRegular = this.joinVisibilityData(visibilityDataSet, section, this.viewport, 1);
                var visibilityDataLocal = this.joinVisibilityData(visibilityDataSet, section, this.viewport, 0);

                // occlude paths through the section if it's being viewed at an angle where
                // the other side can't possibly be seen
                visibilityDataWide &= getAngleVisibilityMaskWide(this.viewport, section, 2);
                visibilityDataRegular &= getAngleVisibilityMaskWide(this.viewport, section, 1);
                visibilityDataLocal &= getAngleVisibilityMaskLocal(this.viewport, section);

                // When using occlusion culling, we can only traverse into neighbors for which there is a path of
                // visibility through this chunk. This is determined by taking all the incoming paths to this chunk and
                // creating a union of the outgoing paths from those.

                outgoingWide = VisibilityEncoding.getConnections(visibilityDataWide, incomingDirectionsWide);
                outgoingRegular = VisibilityEncoding.getConnections(visibilityDataRegular, incomingDirectionsRegular);
                outgoingLocal = VisibilityEncoding.getConnections(visibilityDataLocal, incomingDirectionsLocal);
            } else {
                // Not using any occlusion culling, so traversing in any direction is legal.
                outgoingWide = GraphDirectionSet.ALL;
                outgoingRegular = GraphDirectionSet.ALL;
                outgoingLocal = GraphDirectionSet.ALL;
            }

            // We can only traverse *outwards* from the center of the graph search, so mask off any invalid directions.
            outgoingWide &= getOutwardDirectionsWide(origin, section);
            outgoingRegular &= getOutwardDirectionsRegular(origin, section);
            outgoingLocal &= getOutwardDirectionsRegular(origin, section);

            visitNeighbors(writeQueue, section, outgoingWide, outgoingRegular, outgoingLocal, this.inBoundsOrigin);
        }
    }

    private long joinVisibilityData(long[] visibilityDataSet, RenderSection section, Viewport viewport, int width) {
        if (visibilityDataSet.length == 1) {
            return visibilityDataSet[0];
        }

        int directionSets;
        if (width == 0) {
            directionSets = getDirectionSetsLocal(viewport, section);
        } else {
            directionSets = getDirectionSetsWide(viewport, section, width);
        }

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

    private void visitNeighbors(WriteQueue<RenderSection> queue, RenderSection section, int outgoingWide, int outgoingRegular, int outgoingLocal, SectionPos origin) {
        var adjacentMask = section.getAdjacentMask();
        outgoingWide &= adjacentMask;

        // Check if there are any valid connections left, and if not, early-exit.
        // if wide doesn't have any connections, the others won't either
        if (outgoingWide == GraphDirectionSet.NONE) {
            return;
        }
        outgoingRegular &= adjacentMask;
        outgoingLocal &= adjacentMask;

        var originSection = origin == null ? null : section;

        // the viewpoint is outside the world, so the angle computations relying on propagating angle information
        // from the origin section to the others won't work.
        tryVisitNode(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, section.adjacentDown, GraphDirection.DOWN);
        tryVisitNode(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, section.adjacentUp, GraphDirection.UP);
        tryVisitNode(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, section.adjacentNorth, GraphDirection.NORTH);
        tryVisitNode(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, section.adjacentSouth, GraphDirection.SOUTH);
        tryVisitNode(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, section.adjacentWest, GraphDirection.WEST);
        tryVisitNode(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, section.adjacentEast, GraphDirection.EAST);
    }

    private void tryVisitNode(WriteQueue<RenderSection> queue, RenderSection originSection, int outgoingWide, int outgoingRegular, int outgoingLocal, RenderSection section, int outgoingDirection) {
        // isn't usually null, but can be null if the bfs is happening during loading or unloading of chunks
        if (section == null) {
            return;
        }

        var hasWidePath = GraphDirectionSet.contains(outgoingWide, outgoingDirection);
        var hasRegularPath = GraphDirectionSet.contains(outgoingRegular, outgoingDirection);
        var hasLocalPath = GraphDirectionSet.contains(outgoingLocal, outgoingDirection);

        // perform angular occlusion culling if enabled in general and locally
        // comment out to entirely disable angle-based occlusion culling, the other places are just supporting.
        if (originSection != null && hasRegularPath && !section.intersectSlopes(this.inBoundsOrigin, originSection, this.token)) {
            hasRegularPath = false;
            hasLocalPath = false;
        }

        if (!(hasWidePath || hasRegularPath || hasLocalPath)) {
            // no path to this neighbor from the current section, so skip it
            return;
        }

        visitNode(queue, section, outgoingDirection, hasLocalPath, hasRegularPath, hasWidePath);
    }

    private void visitNode(WriteQueue<RenderSection> queue, RenderSection section, int outgoingDirection, boolean hasLocalPath, boolean hasRegularPath, boolean hasWidePath) {
        if (section.getSearchToken() != this.token) {
            // This is the first time we are visiting this section during the given token, so we must
            // reset the state.
            section.resetOnFirstVisit(this.token);

            // origin point of the chunk's bounding box (in view space)
            CameraTransform camera = this.viewport.getTransform();
            int ox = section.getOriginX() - camera.intX;
            int oy = section.getOriginY() - camera.intY;
            int oz = section.getOriginZ() - camera.intZ;

            // coordinates of the point to compare (in view space)
            // this is the closest point within the bounding box to the center (0, 0, 0)
            float dx = nearestToZero(ox - 1, ox + 17) - camera.fracX;
            float dy = nearestToZero(oy - 1, oy + 17) - camera.fracY;
            float dz = nearestToZero(oz - 1, oz + 17) - camera.fracZ;
            float xzThreshold = (dx * dx) + (dz * dz);
            float yThreshold = Math.abs(dy);

            // vanilla's "cylindrical fog" algorithm
            // max(length(distance.xz), abs(distance.y))
            if (testDistance(xzThreshold, yThreshold, this.searchDistanceRegular)) {
                queue.enqueue(section);

                if (!(testDistance(xzThreshold, yThreshold, this.searchDistanceLocal) && isWithinFrustum(this.viewport, section))) {
                    section.blockLocalIncoming();
                    hasLocalPath = false;
                }
            }
        }

        var incomingSet = GraphDirectionSet.of(GraphDirection.opposite(outgoingDirection));
        if (hasWidePath) {
            section.addIncomingDirectionsWide(incomingSet);
            if (hasRegularPath) {
                section.addIncomingDirectionsRegular(incomingSet);
                if (hasLocalPath) {
                    section.addIncomingDirectionsLocal(incomingSet);
                }
            }
        }
    }

    private static boolean testDistance(float xzThreshold, float yThreshold, float maxDistance) {
        return (xzThreshold < (maxDistance * maxDistance)) && (yThreshold < maxDistance);
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

    private void visitAll(RenderSection section) {
        this.visitorWide.visit(section, true);
        this.visitorRegular.visit(section, true);
        this.visitorLocal.visit(section, true);
    }

    // This method visits sections near the origin that are not in the path of the graph traversal
    // but have bounding boxes that may intersect with the frustum. It does this additional check
    // for all neighboring, even diagonally neighboring, sections around the origin to render them
    // if their extended bounding box is visible, and they may render large models that extend
    // outside the 16x16x16 base volume of the section.
    private void addNearbySections(Viewport viewport) {
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
                    if (section != null && section.getSearchToken() != this.token && isWithinNearbySectionFrustum(viewport, section)) {
                        // reset state on first visit, but don't enqueue
                        section.resetOnFirstVisit(this.token);

                        this.visitAll(section);
                    }
                }
            }
        }
    }

    private void init(WriteQueue<RenderSection> queue) {
        if (this.origin.getY() < this.level.getMinSectionY()) {
            // below the level
            this.outOfWorldRadius = 0;
            this.outOfWorldHeight = this.level.getMinSectionY();
            this.outOfWorldDirection = GraphDirection.DOWN;
        } else if (this.origin.getY() > this.level.getMaxSectionY()) {
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
        var originSection = this.getRenderSection(this.origin.getX(), this.origin.getY(), this.origin.getZ());

        if (originSection == null) {
            return;
        }

        originSection.setOriginAngles();
        originSection.resetOnFirstVisit(this.token);

        this.visitAll(originSection);

        int outgoingWide, outgoingRegular, outgoingLocal;

        if (this.useOcclusionCulling) {
            // Since the camera is located inside this chunk, there are no "incoming" directions. So we need to instead
            // find any possible paths out of this chunk and enqueue those neighbors.
            var visibilityDataSet = originSection.getVisibilityData();
            if (visibilityDataSet == null) {
                // No visibility data, so we can't traverse into any neighbors.
                return;
            }

            var visibilityDataWide = this.joinVisibilityData(visibilityDataSet, originSection, this.viewport, 2);
            var visibilityDataRegular = this.joinVisibilityData(visibilityDataSet, originSection, this.viewport, 1);
            var visibilityDataLocal = this.joinVisibilityData(visibilityDataSet, originSection, this.viewport, 0);

            outgoingWide = VisibilityEncoding.getConnections(visibilityDataWide);
            outgoingRegular = VisibilityEncoding.getConnections(visibilityDataRegular);
            outgoingLocal = VisibilityEncoding.getConnections(visibilityDataLocal);
        } else {
            // Occlusion culling is disabled, so we can traverse into any neighbor.
            outgoingWide = GraphDirectionSet.ALL;
            outgoingRegular = GraphDirectionSet.ALL;
            outgoingLocal = GraphDirectionSet.ALL;
        }

        visitNeighbors(queue, originSection, outgoingWide, outgoingRegular, outgoingLocal, this.origin);
    }

    // Enqueues sections that are inside the viewport using diamond spiral iteration to avoid sorting and ensure a
    // consistent order. Innermost layers are enqueued first. Within each layer, iteration starts at the northernmost
    // section and proceeds counterclockwise (N->W->S->E).
    private boolean initOutsideWorldHeight(WriteQueue<RenderSection> queue) {
        var radius = Mth.floor(this.searchDistanceRegular / 16.0f);
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

        if (section == null) {
            return;
        }

        this.visitNode(queue, section, direction, true, true, true);
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(SectionPos.asLong(x, y, z));
    }

    private RenderSection getRenderSection(SectionPos pos) {
        return this.sections.get(pos.asLong());
    }
}
