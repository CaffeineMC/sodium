package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.textures.GpuSampler;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.render.chunk.async.CullTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation.*;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJob;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkJobResult;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.*;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.*;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior.PriorityMode;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.NoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.CameraMovement;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.RemovableMultiForest;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.render.util.RenderAsserts;
import net.caffeinemc.mods.sodium.client.render.viewport.CameraTransform;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenderSectionManager {
    private static final float NEARBY_REBUILD_DISTANCE = Mth.square(16.0f);
    private static final float IMMEDIATE_PRESENT_DISTANCE = Mth.square(64.0f);
    private static final float NEARBY_SORT_DISTANCE = Mth.square(25.0f);

    private static final float FRAME_DURATION_UPLOAD_FRACTION = 0.1f;
    private static final long MIN_UPLOAD_DURATION_BUDGET = 2_000_000L; // 2ms

    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final ConcurrentLinkedDeque<ChunkJobResult<? extends BuilderTaskOutput>> buildResults = new ConcurrentLinkedDeque<>();
    private final JobDurationEstimator jobDurationEstimator = new JobDurationEstimator();
    private final MeshTaskSizeEstimator meshTaskSizeEstimator;
    private final UploadDurationEstimator jobUploadDurationEstimator = new UploadDurationEstimator();
    private ChunkJobCollector lastBlockingCollector;
    private int thisFrameBlockingTasks;
    private int nextFrameBlockingTasks;
    private int deferredTasks;

    private final ChunkRenderer chunkRenderer;

    private final ClientLevel level;

    private final ReferenceSet<RenderSection> sectionsWithGlobalEntities = new ReferenceOpenHashSet<>();

    private final OcclusionCuller occlusionCuller;

    private final int renderDistance;
    private final SortBehavior sortBehavior;

    private final SortTriggering sortTriggering;

    @NonNull
    private SortedRenderLists renderLists;

    private DeferredTaskList frustumTaskLists;
    private DeferredTaskList globalTaskLists;
    private final EnumMap<DeferMode, ReferenceLinkedOpenHashSet<RenderSection>> importantTasks;

    private int frame;
    private long lastFrameDuration = -1;
    private long averageFrameDuration = -1;
    private long lastFrameAtTime = System.nanoTime();
    private static final float FRAME_DURATION_UPDATE_RATIO = 0.05f;

    private boolean needsGraphUpdate = true;
    private boolean needsRenderListUpdate = true;
    private boolean cameraChanged = false;

    private @Nullable Vector3dc cameraPosition;

    private final ExecutorService asyncCullExecutor = Executors.newSingleThreadExecutor(RenderSectionManager::makeAsyncCullThread);
    private CullTask pendingTask = null;

    private SectionTree renderTree = null;
    private final Map<CullType, SectionTree> cullResults = new EnumMap<>(CullType.class);
    private final RemovableMultiForest renderableSectionTree;

    private final AsyncCameraTimingControl cameraTimingControl = new AsyncCameraTimingControl();

    public RenderSectionManager(ClientLevel level, int renderDistance, SortBehavior sortBehavior, CommandList commandList) {
        this.meshTaskSizeEstimator = new MeshTaskSizeEstimator(level);

        this.chunkRenderer = new DefaultChunkRenderer(RenderDevice.INSTANCE, ChunkMeshFormats.COMPACT);

        this.level = level;
        this.builder = new ChunkBuilder(level, ChunkMeshFormats.COMPACT);

        this.renderDistance = renderDistance;
        this.sortBehavior = sortBehavior;

        if (this.sortBehavior != SortBehavior.OFF) {
            this.sortTriggering = new SortTriggering();
        } else {
            this.sortTriggering = null;
        }

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.level);

        this.renderLists = SortedRenderLists.empty();
        this.occlusionCuller = new OcclusionCuller(Long2ReferenceMaps.unmodifiable(this.sectionByPosition), this.level);

        this.renderableSectionTree = new RemovableMultiForest(renderDistance);

        this.importantTasks = new EnumMap<>(DeferMode.class);
        for (var deferMode : DeferMode.values()) {
            this.importantTasks.put(deferMode, new ReferenceLinkedOpenHashSet<>());
        }
    }

    public void prepareFrame(Vector3dc cameraPosition) {
        this.cameraPosition = cameraPosition;

        var now = System.nanoTime();
        this.lastFrameDuration = now - this.lastFrameAtTime;
        this.lastFrameAtTime = now;
        if (this.averageFrameDuration == -1) {
            this.averageFrameDuration = this.lastFrameDuration;
        } else {
            this.averageFrameDuration = MathUtil.exponentialMovingAverage(this.averageFrameDuration, this.lastFrameDuration, FRAME_DURATION_UPDATE_RATIO);
        }
        this.averageFrameDuration = Mth.clamp(this.averageFrameDuration, 1_000_100, 100_000_000);
    }

    public void prepareRender() {
        this.frame += 1;
        this.needsRenderListUpdate |= this.cameraChanged;
    }

    public void prepareRenderTrees(Camera camera, Viewport viewport, FogParameters fogParameters, boolean spectator) {
        // cancel task if not in progress
        if (this.pendingTask != null && this.pendingTask.cancelIfNotStarted()) {
            this.pendingTask = null;
        }

        // consume the results of completed tasks
        this.consumeTaskResults(false);

        // discard unusable present and pending frustum-tested trees
        if (this.cameraChanged) {
            this.cullResults.remove(CullType.LOCAL);
        }

        // if the origin exists in the graph, schedule new async culling task
        if (!this.isOutOfGraph(viewport.getChunkCoord()) && (this.cameraChanged || this.needsGraphUpdate)) {
            this.scheduleAsyncWork(camera, viewport, fogParameters, spectator);
            this.needsGraphUpdate = false;
        }
    }

    public void finalizeRenderLists(Camera camera, Viewport viewport, FogParameters fogParameters, boolean updateChunksImmediately) {
        var syncRender = this.cameraTimingControl.getShouldRenderSync(camera);
        if (updateChunksImmediately || syncRender && (this.needsGraphUpdate || this.needsRenderListUpdate)) {
            this.renderOutOfGraph(viewport, fogParameters);
        } else if (this.needsRenderListUpdate) {
            this.readRenderListFromTree(viewport, fogParameters);
        }

        this.needsRenderListUpdate = false;
        this.cameraChanged = false;
    }

    private void consumeTaskResults(boolean waitForCompletion) {
        if (this.pendingTask == null) {
            return;
        }

        // if there's a waiting viewport, don't skip unfinished task
        if (!waitForCompletion && !this.pendingTask.isDone()) {
            return;
        }

        var result = this.pendingTask.getResult();
        var treeLocal = result.getCullTreeLocal();
        var treeRegular = result.getCullTreeRegular();
        var treeWide = result.getCullTreeWide();
        this.cullResults.put(CullType.LOCAL, treeLocal);
        this.cullResults.put(CullType.REGULAR, treeRegular);
        this.cullResults.put(CullType.WIDE, treeWide);

        this.globalTaskLists = result.getGlobalTaskLists();
        this.frustumTaskLists = result.getFrustumTaskLists();

        this.needsRenderListUpdate = true;
        this.pendingTask = null;
    }

    private static Thread makeAsyncCullThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName("Sodium Async Cull Thread");
        return thread;
    }

    private void scheduleAsyncWork(Camera camera, Viewport viewport, FogParameters fogParameters, boolean spectator) {
        if (this.pendingTask != null) {
            return;
        }

        // submit cull task if there's none running currently
        var searchDistanceRegular = this.getSearchDistanceForCullType(CullType.REGULAR, fogParameters);
        var searchDistanceLocal = this.getSearchDistanceForCullType(CullType.LOCAL, fogParameters);

        var useOcclusionCulling = this.shouldUseOcclusionCulling(camera, spectator);
        this.pendingTask = new CullTask(viewport, searchDistanceRegular, searchDistanceLocal, this.frame, this.occlusionCuller, useOcclusionCulling, this.level);
        this.pendingTask.submitTo(this.asyncCullExecutor);
    }

    private static final LongArrayList timings = new LongArrayList();

    private SectionTree findBestTree(Viewport viewport, FogParameters fogParameters) {
        for (var type : CullType.NARROW_TO_WIDE) {
            var tree = this.cullResults.get(type);
            if (tree == null) {
                continue;
            }

            float searchDistance = this.getSearchDistanceForCullType(type, fogParameters);
            if (tree.isValidFor(viewport, searchDistance)) {
                return tree;
            }
        }

        return null;
    }

    private void readRenderListFromTree(Viewport viewport, FogParameters fogParameters) {
        // pick the narrowest available tree
        var bestTree = this.findBestTree(viewport, fogParameters);

        // use out-of-graph fallback if the origin section is not loaded and there's no valid tree (missing origin section, empty world)
        if (bestTree == null && this.isOutOfGraph(viewport.getChunkCoord())) {
            this.renderOutOfGraph(viewport, fogParameters);
            return;
        }

        // wait for pending tasks to maybe supply a valid tree if there's no current tree (first frames after initial load/reload)
        if (bestTree == null) {
            this.consumeTaskResults(true);
            bestTree = this.findBestTree(viewport, fogParameters);
        }

        if (bestTree == null) {
            this.renderOutOfGraph(viewport, fogParameters);
            return;
        }

        var start = System.nanoTime();

        var visibleCollector = new VisibleChunkCollectorAsync(this.regions, this.frame);
        bestTree.traverse(visibleCollector, viewport, this.getSearchDistance(fogParameters));
        this.renderLists = visibleCollector.createRenderLists(viewport);

        var end = System.nanoTime();
        var time = end - start;
        timings.add(time);
        if (timings.size() >= 1000) {
            var totalAverage = (long) timings.longStream().average().orElse(0);
            // average with removal of outliers
            var sortedTimings = timings.longStream().sorted().toArray();
            var trimCount = (int) (timings.size() * 0.1);
            var sum = 0L;
            for (int i = trimCount; i < sortedTimings.length - trimCount; i++) {
                sum += sortedTimings[i];
            }
            var average = sum / (sortedTimings.length - trimCount * 2);
            var sectionsWithGeometry = visibleCollector.getUnsortedRenderLists().stream().mapToInt(ChunkRenderList::getSectionsWithGeometryCount).sum();
            if (sectionsWithGeometry == 0) {
                sectionsWithGeometry = 1;
            }
            System.out.println("Render list culling generation took " + average / 1000 + "µs (" + totalAverage / 1000 + "µs raw, " + totalAverage / sectionsWithGeometry + "ns per section) over " + timings.size() + " samples");
            timings.clear();
        }

        this.renderTree = bestTree;
    }

    private void renderOutOfGraph(Viewport viewport, FogParameters fogParameters) {
        var searchDistance = this.getSearchDistance(fogParameters);
        var visitor = new FallbackVisibleChunkCollector(viewport, searchDistance, this.sectionByPosition, this.regions, this.frame);

        this.renderableSectionTree.prepareForTraversal();
        this.renderableSectionTree.traverse(visitor, viewport, searchDistance);

        this.renderLists = visitor.createRenderLists(viewport);
        this.frustumTaskLists = visitor.getPendingTaskLists();
        this.globalTaskLists = null;
        this.renderTree = null;
    }

    private boolean isOutOfGraph(SectionPos pos) {
        var sectionY = pos.getY();
        return this.level.getMinSectionY() <= sectionY && sectionY <= this.level.getMaxSectionY() && !this.sectionByPosition.containsKey(pos.asLong());
    }

    public void markGraphDirty() {
        this.needsGraphUpdate = true;
    }

    public void notifyChangedCamera() {
        this.cameraChanged = true;
    }

    public boolean needsUpdate() {
        return this.needsGraphUpdate;
    }

    private float getSearchDistanceForCullType(CullType cullType, FogParameters fogParameters) {
        if (cullType.isFogCulled) {
            return this.getSearchDistance(fogParameters);
        } else {
            return this.getRenderDistance();
        }
    }

    private float getSearchDistance(FogParameters fogParameters) {
        float distance;

        if (SodiumClientMod.options().performance.useFogOcclusion) {
            distance = this.getEffectiveRenderDistance(fogParameters);
        } else {
            distance = this.getRenderDistance();
        }

        return distance;
    }

    private boolean shouldUseOcclusionCulling(Camera camera, boolean spectator) {
        final boolean useOcclusionCulling;
        BlockPos origin = camera.blockPosition();

        if (spectator && this.level.getBlockState(origin)
                .isSolidRender()) {
            useOcclusionCulling = false;
        } else {
            useOcclusionCulling = Minecraft.getInstance().smartCull;
        }
        return useOcclusionCulling;
    }

    public void beforeSectionUpdates() {
        this.renderableSectionTree.ensureCapacity(this.getRenderDistance());
    }

    public void onSectionAdded(int x, int y, int z) {
        long key = SectionPos.asLong(x, y, z);

        if (this.sectionByPosition.containsKey(key)) {
            return;
        }

        RenderRegion region = this.regions.createForChunk(x, y, z);

        RenderSection renderSection = new RenderSection(region, x, y, z);
        region.addSection(renderSection);

        this.sectionByPosition.put(key, renderSection);

        ChunkAccess chunk = this.level.getChunk(x, z);
        LevelChunkSection section = chunk.getSections()[this.level.getSectionIndexFromSectionY(y)];

        if (section.hasOnlyAir()) {
            this.updateSectionInfo(renderSection, BuiltSectionInfo.EMPTY);
        } else {
            this.renderableSectionTree.add(renderSection);
            renderSection.setPendingUpdate(ChunkUpdateTypes.INITIAL_BUILD, this.lastFrameAtTime);
        }

        this.connectNeighborNodes(renderSection);

        // force update to schedule build task
        this.markGraphDirty();
    }

    public void onSectionRemoved(int x, int y, int z) {
        long sectionPos = SectionPos.asLong(x, y, z);
        RenderSection section = this.sectionByPosition.remove(sectionPos);

        if (section == null) {
            return;
        }

        this.renderableSectionTree.remove(x, y, z);

        if (section.getTranslucentData() != null) {
            this.sortTriggering.removeSection(section.getTranslucentData(), sectionPos);
        }

        RenderRegion region = section.getRegion();

        if (region != null) {
            region.removeSection(section);
        }

        this.disconnectNeighborNodes(section);
        this.updateSectionInfo(section, null);

        section.delete();

        // force update to remove section from render lists
        this.markGraphDirty();
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z, FogParameters fogParameters, GpuSampler terrainSampler) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.renderLists, pass, new CameraTransform(x, y, z), fogParameters, this.sortBehavior != SortBehavior.OFF, terrainSampler);

        commandList.flush();
    }

    public void tickVisibleRenders() {
        Iterator<ChunkRenderList> it = this.renderLists.iterator();

        while (it.hasNext()) {
            ChunkRenderList renderList = it.next();

            var region = renderList.getRegion();
            var iterator = renderList.sectionsWithSpritesIterator();

            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {
                var sprites = region.getAnimatedSprites(iterator.nextByteAsInt());

                if (sprites == null) {
                    continue;
                }

                for (TextureAtlasSprite sprite : sprites) {
                    SpriteUtil.INSTANCE.markSpriteActive(sprite);
                }
            }
        }
    }

    private boolean isSectionEmpty(int x, int y, int z) {
        long key = SectionPos.asLong(x, y, z);
        RenderSection section = this.sectionByPosition.get(key);

        if (section == null) {
            return true;
        }

        return !section.needsRender();
    }

    // renderTree is not necessarily frustum-filtered but that is ok since the caller makes sure to eventually also perform a frustum test on the box being tested (see EntityRendererMixin)
    public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
        return this.renderTree == null || this.renderTree.isBoxVisible(x1, y1, z1, x2, y2, z2, this::isSectionEmpty);
    }

    public void processChunkBuilds(Viewport viewport) {
        var results = this.collectChunkBuildResults();

        if (results.isEmpty()) {
            return;
        }

        // only mark as needing a graph update if the uploads could have changed the graph
        // (sort results never change the graph)
        // generally there's no sort results without a camera movement, which would also trigger
        // a graph update, but it can sometimes happen because of async task execution
        if (this.processChunkBuildResults(results, viewport)) {
            this.markGraphDirty();
        }

        for (var result : results) {
            result.destroy();
        }
    }

    private boolean isSectionFrustumVisible(Viewport viewport, RenderSection section) {
        // unloaded sections are considered visible as to not be an impossible requirement for immediate presentation
        return section == null || this.renderTree == null || this.renderTree.isSectionVisible(viewport, section);
    }

    private boolean isSectionImmediatePresentationCandidate(Viewport viewport, RenderSection section) {
        if (this.cameraPosition == null) {
            return false;
        }
        var distanceSquared = section.getSquaredDistance(
                (float) this.cameraPosition.x(),
                (float) this.cameraPosition.y(),
                (float) this.cameraPosition.z()
        );

        if (distanceSquared < NEARBY_REBUILD_DISTANCE) {
            return true;
        }

        return distanceSquared < IMMEDIATE_PRESENT_DISTANCE &&
                // check that visible or adjacent to a visible section
                (this.isSectionFrustumVisible(viewport, section)
                        || this.isSectionFrustumVisible(viewport, section.adjacentDown)
                        || this.isSectionFrustumVisible(viewport, section.adjacentUp)
                        || this.isSectionFrustumVisible(viewport, section.adjacentNorth)
                        || this.isSectionFrustumVisible(viewport, section.adjacentSouth)
                        || this.isSectionFrustumVisible(viewport, section.adjacentWest)
                        || this.isSectionFrustumVisible(viewport, section.adjacentEast));
    }

    private boolean processChunkBuildResults(ArrayList<BuilderTaskOutput> results, Viewport viewport) {
        var filtered = filterChunkBuildResults(results);

        var start = System.nanoTime();
        this.regions.uploadResults(RenderDevice.INSTANCE.createCommandList(), filtered);
        var uploadDuration = System.nanoTime() - start;

        // prepare list of pending present patches if there are pending tasks that will need patches
        List<RenderSection> pendingPresentPatches = null;
        if (this.pendingTask != null) {
            pendingPresentPatches = new ReferenceArrayList<>();
        }

        boolean touchedSectionInfo = false;
        long totalUploadSize = 0;
        for (var result : filtered) {
            var resultSize = result.getResultSize();
            RenderSection section = result.render;
            var job = section.getRunningJob();

            TranslucentData oldData = section.getTranslucentData();
            if (result instanceof ChunkBuildOutput chunkBuildOutput) {
                touchedSectionInfo |= updateWithResult(viewport, section, chunkBuildOutput, job, pendingPresentPatches);

                section.setLastMeshResultSize(resultSize);
                this.meshTaskSizeEstimator.addData(this.meshTaskSizeEstimator.resultForSection(section, resultSize));

                if (chunkBuildOutput.translucentData != null) {
                    this.sortTriggering.integrateTranslucentData(oldData, chunkBuildOutput.translucentData, this.cameraPosition, this::scheduleSort);

                    // a rebuild always generates new translucent data which means applyTriggerChanges isn't necessary
                    section.setTranslucentData(chunkBuildOutput.translucentData);
                }
            } else if (result instanceof ChunkSortOutput sortOutput &&
                    sortOutput.getDynamicSorter() != null &&
                    section.getTranslucentData() instanceof DynamicTopoData data) {
                this.sortTriggering.applyTriggerChanges(data, sortOutput.getDynamicSorter(), section.getPosition(), this.cameraPosition);
            }

            // clear the running job if this job is the most recent submitted job for this section
            if (job != null && result.submitTime >= section.getLastSubmittedFrame()) {
                section.setRunningJob(null);
            }

            section.setLastUploadFrame(result.submitTime);

            totalUploadSize += resultSize;
        }

        this.meshTaskSizeEstimator.updateModels();

        // insert and update the upload duration estimator with the total upload size,
        // since we don't know which task took how long and the time it takes to upload is not independent between tasks
        // we take the average size and duration
        if (!filtered.isEmpty()) {
            this.jobUploadDurationEstimator.addData(new UploadDuration(uploadDuration / filtered.size(), totalUploadSize / filtered.size()));
            this.jobUploadDurationEstimator.updateModels();
        }

        if (pendingPresentPatches != null && !pendingPresentPatches.isEmpty() &&
                this.pendingTask != null) {
            this.pendingTask.registerPresentPatches(pendingPresentPatches);
        }

        return touchedSectionInfo;
    }

    private boolean updateWithResult(Viewport viewport, RenderSection section, ChunkBuildOutput chunkBuildOutput, ChunkJob job, List<RenderSection> pendingPresentPatches) {
        var index = section.getSectionIndex();
        var prevFlags = section.getRegion().getSectionFlags(index);

        var touchedSectionInfo = this.updateSectionInfo(section, chunkBuildOutput.info);

        // if result was blocking (or is approximately visible) and section is now newly renderable, force render it since it's probably a newly uncovered chunk.
        // This also fixes flickering issues with pistons moving blocks and switching between being a mesh and a BE.
        if (this.renderTree != null && job != null &&
                (job.isBlocking() || this.isSectionImmediatePresentationCandidate(viewport, section)) &&
                RenderSectionFlags.renderingMoreTypesNow(prevFlags, chunkBuildOutput.info.flags)) {
            var chunkX = section.getChunkX();
            var chunkY = section.getChunkY();
            var chunkZ = section.getChunkZ();

            for (var tree : this.cullResults.values()) {
                if (tree.patchMarkPresent(chunkX, chunkY, chunkZ)) {
                    this.needsRenderListUpdate = true;
                }
            }

            // collect present patches if we need to
            if (pendingPresentPatches != null) {
                pendingPresentPatches.add(section);
            }
        }

        return touchedSectionInfo;
    }

    private boolean updateSectionInfo(RenderSection render, BuiltSectionInfo info) {
        if (info == null || !RenderSectionFlags.needsRender(info.flags)) {
            this.renderableSectionTree.remove(render);
        } else {
            this.renderableSectionTree.add(render);
        }

        var infoChanged = render.setInfo(info);

        if (info == null || ArrayUtils.isEmpty(info.globalBlockEntities)) {
            return this.sectionsWithGlobalEntities.remove(render) || infoChanged;
        } else {
            return this.sectionsWithGlobalEntities.add(render) || infoChanged;
        }
    }

    private static List<BuilderTaskOutput> filterChunkBuildResults(ArrayList<BuilderTaskOutput> outputs) {
        var map = new Reference2ReferenceLinkedOpenHashMap<RenderSection, BuilderTaskOutput>();

        for (var output : outputs) {
            // throw out outdated or duplicate outputs
            if (output.render.isDisposed() || output.render.getLastUploadFrame() > output.submitTime) {
                continue;
            }

            var render = output.render;
            var previous = map.get(render);

            if (previous == null || previous.submitTime < output.submitTime) {
                map.put(render, output);
            }
        }

        return new ArrayList<>(map.values());
    }

    private ArrayList<BuilderTaskOutput> collectChunkBuildResults() {
        ArrayList<BuilderTaskOutput> results = new ArrayList<>();

        ChunkJobResult<? extends BuilderTaskOutput> result;

        while ((result = this.buildResults.poll()) != null) {
            results.add(result.unwrap());
            var jobEffort = result.getJobEffort();
            if (jobEffort != null) {
                this.jobDurationEstimator.addData(jobEffort);
            }
        }

        this.jobDurationEstimator.updateModels();

        return results;
    }

    public void cleanupAndFlip() {
        this.sectionCache.cleanup();
        this.regions.update();
    }

    public void updateChunks(Viewport viewport, boolean updateImmediately) {
        this.thisFrameBlockingTasks = 0;
        this.nextFrameBlockingTasks = 0;
        this.deferredTasks = 0;

        var thisFrameBlockingCollector = this.lastBlockingCollector;
        this.lastBlockingCollector = null;
        if (thisFrameBlockingCollector == null) {
            thisFrameBlockingCollector = new ChunkJobCollector(this.buildResults::add);
        }

        if (updateImmediately) {
            // for a perfect frame where everything is finished use the last frame's blocking collector
            // and add all tasks to it so that they're waited on
            this.submitSectionTasks(thisFrameBlockingCollector, thisFrameBlockingCollector, thisFrameBlockingCollector, UnlimitedResourceBudget.INSTANCE, viewport);

            this.thisFrameBlockingTasks = thisFrameBlockingCollector.getSubmittedTaskCount();
            thisFrameBlockingCollector.awaitCompletion(this.builder);
        } else {
            var remainingDuration = this.builder.getTotalRemainingDuration(this.averageFrameDuration);

            // an estimator is used estimate task duration and limit the execution time to the available worker capacity.
            // separately, tasks are limited by their estimated upload size and duration.
            var uploadBudget = new LimitedResourceBudget(
                    Math.max((long) (this.averageFrameDuration * FRAME_DURATION_UPLOAD_FRACTION), MIN_UPLOAD_DURATION_BUDGET),
                    this.regions.getStagingBuffer().getUploadSizeLimit(this.averageFrameDuration));

            var nextFrameBlockingCollector = new ChunkJobCollector(this.buildResults::add);
            var deferredCollector = new ChunkJobCollector(remainingDuration, this.buildResults::add);

            this.submitSectionTasks(thisFrameBlockingCollector, nextFrameBlockingCollector, deferredCollector, uploadBudget, viewport);

            this.thisFrameBlockingTasks = thisFrameBlockingCollector.getSubmittedTaskCount();
            this.nextFrameBlockingTasks = nextFrameBlockingCollector.getSubmittedTaskCount();
            this.deferredTasks = deferredCollector.getSubmittedTaskCount();

            // wait on this frame's blocking collector which contains the important tasks from this frame
            // and semi-important tasks from the last frame
            thisFrameBlockingCollector.awaitCompletion(this.builder);

            // store the semi-important collector to wait on it in the next frame
            this.lastBlockingCollector = nextFrameBlockingCollector;
        }
    }

    private void submitSectionTasks(
            ChunkJobCollector importantCollector, ChunkJobCollector semiImportantCollector, ChunkJobCollector deferredCollector, UploadResourceBudget uploadBudget, Viewport viewport) {
        submitImportantSectionTasks(importantCollector, uploadBudget, DeferMode.ZERO_FRAMES, viewport);
        submitImportantSectionTasks(semiImportantCollector, uploadBudget, DeferMode.ONE_FRAME, viewport);
        submitImportantSectionTasks(deferredCollector, uploadBudget, DeferMode.ALWAYS, viewport);

        submitDeferredSectionTasks(deferredCollector, uploadBudget);
    }

    private static final LongPriorityQueue EMPTY_TASK_QUEUE = new LongPriorityQueue() {
        @Override
        public void enqueue(long x) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long dequeueLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long firstLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public LongComparator comparator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    };

    private void submitDeferredSectionTasks(ChunkJobCollector collector, UploadResourceBudget uploadBudget) {
        LongPriorityQueue frustumQueue = this.frustumTaskLists;
        LongPriorityQueue globalQueue = this.globalTaskLists;
        float frustumPriorityBias = 0;
        float globalPriorityBias = 0;

        if (frustumQueue != null) {
            frustumPriorityBias = this.frustumTaskLists.getCollectorPriorityBias(this.lastFrameAtTime);
        } else {
            frustumQueue = EMPTY_TASK_QUEUE;
        }

        if (globalQueue != null) {
            globalPriorityBias = this.globalTaskLists.getCollectorPriorityBias(this.lastFrameAtTime);
        } else {
            globalQueue = EMPTY_TASK_QUEUE;
        }

        float frustumPriority = Float.POSITIVE_INFINITY;
        float globalPriority = Float.POSITIVE_INFINITY;
        long frustumItem = 0;
        long globalItem = 0;

        while ((!frustumQueue.isEmpty() || !globalQueue.isEmpty()) && collector.hasBudgetRemaining() && uploadBudget.isAvailable()) {
            // get the first item from the non-empty queues and see which one has higher priority.
            // if the priority is not infinity, then the item priority was fetched the last iteration and doesn't need updating.
            if (!frustumQueue.isEmpty() && Float.isInfinite(frustumPriority)) {
                frustumItem = frustumQueue.firstLong();
                frustumPriority = PendingTaskCollector.decodePriority(frustumItem) + frustumPriorityBias;
            }
            if (!globalQueue.isEmpty() && Float.isInfinite(globalPriority)) {
                globalItem = globalQueue.firstLong();
                globalPriority = PendingTaskCollector.decodePriority(globalItem) + globalPriorityBias;
            }

            // pick the task with the higher priority, decode the section, and schedule its task if it exists
            RenderSection section;
            if (frustumPriority < globalPriority) {
                frustumQueue.dequeueLong();
                frustumPriority = Float.POSITIVE_INFINITY;

                section = this.frustumTaskLists.decodeAndFetchSection(this.sectionByPosition, frustumItem);
            } else {
                globalQueue.dequeueLong();
                globalPriority = Float.POSITIVE_INFINITY;

                section = this.globalTaskLists.decodeAndFetchSection(this.sectionByPosition, globalItem);
            }

            if (section != null) {
                submitSectionTask(collector, section, uploadBudget);
            }
        }
    }

    private DeferMode getDeferModeForPendingUpdate(int type) {
        return ChunkUpdateTypes.getDeferMode(type, SodiumClientMod.options().performance.chunkBuildDeferMode, this.sortBehavior.getDeferMode());
    }

    private void submitImportantSectionTasks(ChunkJobCollector collector, UploadResourceBudget uploadBudget, DeferMode deferMode, Viewport viewport) {
        var it = this.importantTasks.get(deferMode).iterator();

        while (it.hasNext() && collector.hasBudgetRemaining() && (deferMode.allowsUnlimitedUploadDuration() || uploadBudget.isAvailable())) {
            var section = it.next();
            var pendingUpdate = section.getPendingUpdate();

            if (pendingUpdate != 0 && this.getDeferModeForPendingUpdate(pendingUpdate) == deferMode && this.shouldPrioritizeTask(section, NEARBY_SORT_DISTANCE)) {
                // isSectionVisible includes a special case for not testing empty sections against the tree as they won't be in it
                if (this.renderTree == null || this.renderTree.isSectionVisible(viewport, section)) {
                    submitSectionTask(collector, section, pendingUpdate, uploadBudget, deferMode == DeferMode.ZERO_FRAMES);
                } else {
                    // don't remove if simply not visible currently but still relevant
                    continue;
                }
            }
            it.remove();
        }
    }

    private void submitSectionTask(ChunkJobCollector collector, @NotNull RenderSection section, UploadResourceBudget uploadBudget) {
        // don't schedule tasks for sections that don't need it anymore,
        // since the pending update it cleared when a task is started, this includes
        // sections for which there's a currently running task.
        var type = section.getPendingUpdate();
        if (type == 0) {
            return;
        }

        submitSectionTask(collector, section, type, uploadBudget, false);
    }

    private void submitSectionTask(ChunkJobCollector collector, @NonNull RenderSection section, int type, UploadResourceBudget uploadBudget, boolean blocking) {
        if (section.isDisposed()) {
            return;
        }

        ChunkBuilderTask<? extends BuilderTaskOutput> task;
        if (ChunkUpdateTypes.isInitialBuild(type) || ChunkUpdateTypes.isRebuild(type)) {
            task = this.createRebuildTask(section, this.frame);

            if (task == null) {
                // if the section is empty or doesn't exist submit this null-task to set the
                // built flag on the render section.
                // It's important to use a NoData instead of null translucency data here in
                // order for it to clear the old data from the translucency sorting system.
                // This doesn't apply to sorting tasks as that would result in the section being
                // marked as empty just because it was scheduled to be sorted and its dynamic
                // data has since been removed. In that case simply nothing is done as the
                // rebuild that must have happened in the meantime includes new non-dynamic
                // index data.
                TranslucentData translucentData = null;
                if (this.sortBehavior != SortBehavior.OFF) {
                    translucentData = NoData.forEmptySection(section.getPosition());
                }
                var result = ChunkJobResult.successfully(new ChunkBuildOutput(
                        section, this.frame, translucentData,
                        BuiltSectionInfo.EMPTY, Collections.emptyMap()));
                this.buildResults.add(result);

                section.setRunningJob(null);
            }
        } else { // implies it's a type of sort task
            task = this.createSortTask(section, this.frame);

            if (task == null) {
                // when a sort task is null it means the render section has no dynamic data and
                // doesn't need to be sorted. Nothing needs to be done.
                section.clearPendingUpdate();
                return;
            }
        }

        if (task != null) {
            var job = this.builder.scheduleTask(task, ChunkUpdateTypes.isImportant(type), collector::onJobFinished, blocking);
            collector.addSubmittedJob(job);

            // consume upload budget in size and duration using estimates
            uploadBudget.consume(job.getEstimatedUploadDuration(), job.getEstimatedSize());

            section.setRunningJob(job);
        }

        section.setLastSubmittedFrame(this.frame);
        section.clearPendingUpdate();
    }

    public @Nullable ChunkBuilderMeshingTask createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = LevelSlice.prepare(this.level, render.getPosition(), this.sectionCache);

        if (context == null) {
            return null;
        }

        var task = new ChunkBuilderMeshingTask(render, frame, this.cameraPosition, context, this.sortBehavior, ChunkUpdateTypes.isRebuildWithSort(render.getPendingUpdate()));
        task.calculateEstimations(this.jobDurationEstimator, this.meshTaskSizeEstimator, this.jobUploadDurationEstimator);
        return task;
    }

    public ChunkBuilderSortingTask createSortTask(RenderSection render, int frame) {
        var task = ChunkBuilderSortingTask.createTask(render, frame, this.cameraPosition);
        if (task != null) {
            task.calculateEstimations(this.jobDurationEstimator, this.meshTaskSizeEstimator, this.jobUploadDurationEstimator);
        }
        return task;
    }

    public void processGFNIMovement(CameraMovement movement) {
        if (this.sortTriggering != null) {
            this.sortTriggering.triggerSections(this::scheduleSort, movement);
        }
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.builder.shutdown(); // stop all the workers, and cancel any tasks

        this.asyncCullExecutor.shutdownNow();

        for (var result : this.collectChunkBuildResults()) {
            result.destroy(); // delete resources for any pending tasks (including those that were cancelled)
        }

        for (var section : this.sectionByPosition.values()) {
            section.delete();
        }

        this.sectionsWithGlobalEntities.clear();

        this.renderLists = SortedRenderLists.empty();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
            this.chunkRenderer.delete(commandList);
        }
    }

    public int getTotalSections() {
        return this.sectionByPosition.size();
    }

    public int getVisibleChunkCount() {
        var sections = 0;
        var iterator = this.renderLists.iterator();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    private boolean upgradePendingUpdate(RenderSection section, int updateType) {
        if (updateType == 0) {
            return false;
        }

        var current = section.getPendingUpdate();
        var joined = ChunkUpdateTypes.join(current, updateType);

        if (joined == current) {
            return false;
        }

        section.setPendingUpdate(joined, this.lastFrameAtTime);

        // when the pending task type changes, and it's important, add it to the list of important tasks
        if (ChunkUpdateTypes.isImportant(joined)) {
            this.importantTasks.get(this.getDeferModeForPendingUpdate(joined)).add(section);
        }

        return true;
    }

    public void scheduleSort(long sectionPos, boolean isDirectTrigger) {
        RenderSection section = this.sectionByPosition.get(sectionPos);

        if (section != null) {
            int pendingUpdate = ChunkUpdateTypes.SORT;
            var priorityMode = this.sortBehavior.getPriorityMode();
            if (priorityMode == PriorityMode.NEARBY && this.shouldPrioritizeTask(section, NEARBY_SORT_DISTANCE) || priorityMode == PriorityMode.ALL) {
                pendingUpdate = ChunkUpdateTypes.join(pendingUpdate, ChunkUpdateTypes.IMPORTANT);
            }

            if (this.upgradePendingUpdate(section, pendingUpdate)) {
                section.prepareTrigger(isDirectTrigger);
            }
        }
    }

    public void scheduleRebuild(int x, int y, int z, boolean playerChanged) {
        RenderAsserts.validateCurrentThread();

        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionByPosition.get(SectionPos.asLong(x, y, z));

        if (section != null && section.isBuilt()) {
            int pendingUpdate;

            if (playerChanged && this.shouldPrioritizeTask(section, NEARBY_REBUILD_DISTANCE)) {
                pendingUpdate = ChunkUpdateTypes.join(ChunkUpdateTypes.REBUILD, ChunkUpdateTypes.IMPORTANT);
            } else {
                pendingUpdate = ChunkUpdateTypes.REBUILD;
            }

            this.upgradePendingUpdate(section, pendingUpdate);
        }
    }

    private boolean shouldPrioritizeTask(RenderSection section, float distance) {
        return this.cameraPosition != null && section.getSquaredDistance(
                (float) this.cameraPosition.x(),
                (float) this.cameraPosition.y(),
                (float) this.cameraPosition.z()
        ) < distance;
    }

    private float getEffectiveRenderDistance(FogParameters fogParameters) {
        var alpha = fogParameters.alpha();
        var environmentalEnd = fogParameters.environmentalEnd();
        var distance = Float.isNaN(environmentalEnd) ? fogParameters.renderEnd() : Math.min(fogParameters.renderEnd(), environmentalEnd);

        var renderDistance = this.getRenderDistance();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (!Mth.equal(alpha, 1.0f)) {
            return renderDistance;
        }

        return Math.min(renderDistance, distance + 0.5f);
    }

    private float getRenderDistance() {
        return this.renderDistance * 16.0f;
    }

    private void connectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + GraphDirection.x(direction),
                    render.getChunkY() + GraphDirection.y(direction),
                    render.getChunkZ() + GraphDirection.z(direction));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), render);
                render.setAdjacentNode(direction, adj);
            }
        }
    }

    private void disconnectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = render.getAdjacent(direction);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), null);
                render.setAdjacentNode(direction, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sectionByPosition.get(SectionPos.asLong(x, y, z));
    }

    public Collection<String> getDebugStrings(boolean verbose) {
        List<String> list = new ArrayList<>();

        int count = 0;

        long geometryDeviceUsed = 0;
        long geometryDeviceAllocated = 0;
        long indexDeviceUsed = 0;
        long indexDeviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            var resources = region.getResources();

            if (resources == null) {
                continue;
            }

            var geometryArena = resources.getGeometryArena();
            geometryDeviceUsed += geometryArena.getDeviceUsedMemory();
            geometryDeviceAllocated += geometryArena.getDeviceAllocatedMemory();

            var indexArena = resources.getIndexArena();
            indexDeviceUsed += indexArena.getDeviceUsedMemory();
            indexDeviceAllocated += indexArena.getDeviceAllocatedMemory();

            count++;
        }

        if (verbose) {
            list.add(String.format("Pools: Geometry %d/%d MiB, Index %d/%d MiB (%d buffers)",
                    MathUtil.toMib(geometryDeviceUsed), MathUtil.toMib(geometryDeviceAllocated),
                    MathUtil.toMib(indexDeviceUsed), MathUtil.toMib(indexDeviceAllocated), count));
            list.add(String.format("Transfer Queue: %s", this.regions.getStagingBuffer().toString()));
        } else {
            list.add(String.format("G:%d/%d I:%d/%d MiB TQ: %s #%d",
                    MathUtil.toMib(geometryDeviceUsed), MathUtil.toMib(geometryDeviceAllocated),
                    MathUtil.toMib(indexDeviceUsed), MathUtil.toMib(indexDeviceAllocated),
                    this.regions.getStagingBuffer().toString(), count));
        }

        if (verbose) {
            list.add(String.format("Chunk Builder: Schd=%02d | Busy=%02d (%04d%%) | Total=%02d",
                    this.builder.getScheduledJobCount(), this.builder.getBusyThreadCount(), (int) (this.builder.getBusyFraction(this.lastFrameDuration) * 100), this.builder.getTotalThreadCount())
            );
        } else {
            list.add(String.format("B: S%02d/B%02d/T%02d",
                    this.builder.getScheduledJobCount(), this.builder.getBusyThreadCount(), this.builder.getTotalThreadCount())
            );
        }

        if (verbose) {
            list.add(String.format("Tasks: N0=%03d | N1=%03d | Def=%03d, Recv=%03d",
                    this.thisFrameBlockingTasks, this.nextFrameBlockingTasks, this.deferredTasks, this.buildResults.size())
            );
        }

        if (verbose && PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment()) {
            var meshTaskParameters = this.jobDurationEstimator.toString(ChunkBuilderMeshingTask.class);
            var sortTaskParameters = this.jobDurationEstimator.toString(ChunkBuilderSortingTask.class);
            var uploadDurationParameters = this.jobUploadDurationEstimator.toString(null);
            list.add(String.format("Duration: Mesh %s, Sort %s, Upload %s", meshTaskParameters, sortTaskParameters, uploadDurationParameters));

            var sizeEstimates = new ReferenceArrayList<String>();
            for (var type : MeshResultSize.SectionCategory.values()) {
                sizeEstimates.add(String.format("%s=%s", type, this.meshTaskSizeEstimator.toString(type)));
            }
            list.add(String.format("Size: %s", String.join(", ", sizeEstimates)));
        }

        if (this.sortBehavior != SortBehavior.OFF) {
            this.sortTriggering.addDebugStrings(list, this.sortBehavior, verbose);
        } else {
            list.add("TS OFF");
        }


        list.add("Async Culling: " + (this.pendingTask == null ?
                "Idle" : this.pendingTask.isDone() ? "Done" : "Running"));

        return list;
    }

    public String getChunksDebugString() {
        // C: visible/total D: distance
        return String.format(
                "C: %d/%d (%s) D: %d",
                this.getVisibleChunkCount(),
                this.getTotalSections(),
                this.getCullTypeName(),
                this.renderDistance);
    }

    private String getCullTypeName() {
        CullType renderTreeCullType = null;
        for (var type : CullType.values()) {
            if (this.cullResults.get(type) == this.renderTree) {
                renderTreeCullType = type;
                break;
            }
        }
        var cullTypeName = "-";
        if (renderTreeCullType != null) {
            cullTypeName = renderTreeCullType.abbreviation;
        }
        return cullTypeName;
    }

    public @NonNull SortedRenderLists getRenderLists() {
        return this.renderLists;
    }

    public boolean isSectionBuilt(int x, int y, int z) {
        var section = this.getRenderSection(x, y, z);
        return section != null && section.isBuilt();
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.level.getMinSectionY(); y <= this.level.getMaxSectionY(); y++) {
            this.onSectionAdded(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.level.getMinSectionY(); y <= this.level.getMaxSectionY(); y++) {
            this.onSectionRemoved(x, y, z);
        }
    }

    public Collection<RenderSection> getSectionsWithGlobalEntities() {
        return ReferenceSets.unmodifiable(this.sectionsWithGlobalEntities);
    }
}
