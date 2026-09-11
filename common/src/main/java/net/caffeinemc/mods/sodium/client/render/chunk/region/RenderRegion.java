package net.caffeinemc.mods.sodium.client.render.chunk.region;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.gpu.arena.ArenaAggregator;
import net.caffeinemc.mods.sodium.client.gpu.arena.RegionAllocatorHandle;
import net.caffeinemc.mods.sodium.client.gpu.device.batch.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.IntPool;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionFlags;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.SectionRenderDataStorage;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

public class RenderRegion {
    public static final int SECTION_VERTEX_COUNT_ESTIMATE = 756;
    public static final int SECTION_INDEX_COUNT_ESTIMATE = (SECTION_VERTEX_COUNT_ESTIMATE / DefaultTerrainRenderPasses.ALL.length / 4) * 6;
    public static final int SECTION_BUFFER_ESTIMATE = SECTION_VERTEX_COUNT_ESTIMATE * ChunkMeshFormats.COMPACT.getVertexFormat().getVertexSize() + SECTION_INDEX_COUNT_ESTIMATE * Integer.BYTES;

    public static final int REGION_WIDTH = 8;
    public static final int REGION_HEIGHT = 4;
    public static final int REGION_LENGTH = 8;

    public static final int REGION_WIDTH_M = RenderRegion.REGION_WIDTH - 1;
    public static final int REGION_HEIGHT_M = RenderRegion.REGION_HEIGHT - 1;
    public static final int REGION_LENGTH_M = RenderRegion.REGION_LENGTH - 1;

    public static final int REGION_WIDTH_SH = Integer.bitCount(REGION_WIDTH_M);
    public static final int REGION_HEIGHT_SH = Integer.bitCount(REGION_HEIGHT_M);
    public static final int REGION_LENGTH_SH = Integer.bitCount(REGION_LENGTH_M);

    public static final int REGION_SIZE = REGION_WIDTH * REGION_HEIGHT * REGION_LENGTH;

    public static final int SHARED_INDEX_DATA_INDEX = -1;

    static {
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_WIDTH));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_HEIGHT));
        Validate.isTrue(MathUtil.isPowerOfTwo(REGION_LENGTH));
    }

    private final ArenaAggregator arenaAggregator;
    private final int x, y, z;

    private final ChunkRenderList renderList;

    private final RenderSection[] sections = new RenderSection[RenderRegion.REGION_SIZE];
    private final byte[] sectionFlags = new byte[RenderRegion.REGION_SIZE];
    private final BlockEntity[] @Nullable [] globalBlockEntities = new BlockEntity[RenderRegion.REGION_SIZE][];
    private final BlockEntity[] @Nullable [] culledBlockEntities = new BlockEntity[RenderRegion.REGION_SIZE][];
    private final TextureAtlasSprite[] @Nullable [] animatedSprites = new TextureAtlasSprite[RenderRegion.REGION_SIZE][];
    private final long creationTime;
    private int sectionCount;

    private final Map<TerrainRenderPass, SectionRenderDataStorage> sectionRenderData = new Reference2ReferenceOpenHashMap<>();
    private DeviceResources resources;

    private final Map<TerrainRenderPass, MultiDrawBatch> cachedBatches = new Reference2ReferenceOpenHashMap<>();
    private int uniqueId = -1;

    public RenderRegion(int x, int y, int z, ArenaAggregator arenaAggregator) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.creationTime = System.currentTimeMillis();

        this.arenaAggregator = arenaAggregator;
        this.renderList = new ChunkRenderList(this);
    }

    public static long key(int x, int y, int z) {
        return SectionPos.asLong(x, y, z);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public int getChunkX() {
        return this.x << REGION_WIDTH_SH;
    }

    public int getChunkY() {
        return this.y << REGION_HEIGHT_SH;
    }

    public int getChunkZ() {
        return this.z << REGION_LENGTH_SH;
    }

    public int getOriginX() {
        return this.getChunkX() << 4;
    }

    public int getOriginY() {
        return this.getChunkY() << 4;
    }

    public int getOriginZ() {
        return this.getChunkZ() << 4;
    }

    public void delete() {
        for (var storage : this.sectionRenderData.values()) {
            storage.delete();
        }

        this.sectionRenderData.clear();

        if (this.resources != null) {
            this.resources.delete();
            this.resources = null;
        }

        Arrays.fill(this.sections, null);

        for (var batch : this.cachedBatches.values()) {
            batch.delete();
        }
        this.cachedBatches.clear();
    }

    public void clearAllCachedBatches() {
        for (var batch : this.cachedBatches.values()) {
            batch.clear();
        }
    }

    public void clearCachedBatchFor(TerrainRenderPass pass) {
        var batch = this.cachedBatches.get(pass);
        if (batch != null) {
            batch.clear();
        }
    }

    public MultiDrawBatch getCachedBatch(TerrainRenderPass pass) {
        MultiDrawBatch batch = this.cachedBatches.get(pass);
        if (batch != null) {
            return batch;
        }

        batch = MultiDrawBatch.newBatch((ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1);
        this.cachedBatches.put(pass, batch);
        return batch;
    }

    public boolean isEmpty() {
        return this.sectionCount == 0;
    }

    public SectionRenderDataStorage getStorage(TerrainRenderPass pass) {
        return this.sectionRenderData.get(pass);
    }

    public SectionRenderDataStorage createStorage(TerrainRenderPass pass) {
        var storage = this.sectionRenderData.get(pass);

        if (storage == null) {
            storage = new SectionRenderDataStorage(pass.isTranslucent());
            this.sectionRenderData.put(pass, storage);
        }

        return storage;
    }

    public void onGeometryBufferChange() {
        for (var storage : this.sectionRenderData.values()) {
            storage.onBufferResized();
        }

        // invalidate the cached batches
        this.clearAllCachedBatches();
    }

    public void onIndexBufferChange() {
        var indexStorage = this.sectionRenderData.get(DefaultTerrainRenderPasses.TRANSLUCENT);
        if (indexStorage != null) {
            indexStorage.onIndexBufferResized();
        }

        // invalidate the cached batches
        this.clearCachedBatchFor(DefaultTerrainRenderPasses.TRANSLUCENT);
    }

    public static int packOwnerIndex(int sectionIndex, int passIndex) {
        return (passIndex << 16) | (sectionIndex & 0xFFFF);
    }

    public static int unpackSectionIndex(int ownerIndex) {
        return ownerIndex & 0xFFFF;
    }

    public static int unpackPassIndex(int ownerIndex) {
        return (ownerIndex >> 16) & 0xFF;
    }

    private void onGeometrySegmentChange(int ownerIndex) {
        var sectionIndex = RenderRegion.unpackSectionIndex(ownerIndex);
        var passIndex = RenderRegion.unpackPassIndex(ownerIndex);
        var storage = this.sectionRenderData.get(DefaultTerrainRenderPasses.ALL[passIndex]);

        storage.onVertexSegmentChanged(sectionIndex);

        this.clearAllCachedBatches();
    }

    private void onIndexSegmentChange(int ownerIndex) {
        var storage = this.sectionRenderData.get(DefaultTerrainRenderPasses.TRANSLUCENT);

        if (ownerIndex == SHARED_INDEX_DATA_INDEX) {
            storage.onSharedIndexSegmentChanged();
        } else {
            var sectionIndex = RenderRegion.unpackSectionIndex(ownerIndex);
            storage.onIndexSegmentChanged(sectionIndex);
        }

        this.clearCachedBatchFor(DefaultTerrainRenderPasses.TRANSLUCENT);
    }

    public void addSection(RenderSection section) {
        var sectionIndex = section.getSectionIndex();
        var prev = this.sections[sectionIndex];

        if (prev != null) {
            throw new IllegalStateException("Section has already been added to the region");
        }

        this.sections[sectionIndex] = section;
        this.sectionCount++;
    }

    public void setSectionRenderState(int id, BuiltSectionInfo info) {
        this.sectionFlags[id] = (byte) (info.flags | RenderSectionFlags.MASK_IS_BUILT);
        this.globalBlockEntities[id] = info.globalBlockEntities;
        this.culledBlockEntities[id] = info.culledBlockEntities;
        this.animatedSprites[id] = info.animatedSprites;
    }

    public void clearSectionRenderState(int id) {
        this.sectionFlags[id] = RenderSectionFlags.NONE;
        this.globalBlockEntities[id] = null;
        this.culledBlockEntities[id] = null;
        this.animatedSprites[id] = null;
    }

    public int getSectionFlags(int id) {
        return this.sectionFlags[id];
    }

    public boolean sectionIsInvisible(int id) {
        return RenderSectionFlags.isInvisible(this.sectionFlags[id]);
    }

    /**
     * Returns the collection of block entities contained by this rendered chunk, which are not part of its culling
     * volume. These entities should always be rendered regardless of the render being visible in the frustum.
     *
     * @param id The section index
     * @return The collection of block entities
     */
    public BlockEntity[] getGlobalBlockEntities(int id) {
        return this.globalBlockEntities[id];
    }

    /**
     * Returns the collection of block entities contained by this rendered chunk.
     *
     * @param id The section index
     * @return The collection of block entities
     */
    public BlockEntity[] getCulledBlockEntities(int id) {
        return this.culledBlockEntities[id];
    }

    /**
     * Returns the collection of animated sprites contained by this rendered chunk section.
     *
     * @param id The section index
     * @return The collection of animated sprites
     */
    public TextureAtlasSprite[] getAnimatedSprites(int id) {
        return this.animatedSprites[id];
    }

    public void removeSection(RenderSection section) {
        var sectionIndex = section.getSectionIndex();
        var prev = this.sections[sectionIndex];

        if (prev == null) {
            throw new IllegalStateException("Section was not loaded within the region");
        } else if (prev != section) {
            throw new IllegalStateException("Tried to remove the wrong section");
        }

        for (var storage : this.sectionRenderData.values()) {
            storage.removeData(sectionIndex);
        }

        this.sections[sectionIndex] = null;
        this.sectionCount--;
    }

    public float getFillFractionInv() {
        return (float) RenderRegion.REGION_SIZE / (float) this.sectionCount;
    }

    public DeviceResources getResources() {
        return this.resources;
    }

    public DeviceResources createResources() {
        if (this.resources == null) {
            this.resources = new DeviceResources(this);
        }

        return this.resources;
    }

    public void update() {
        if (this.resources != null && this.resources.shouldDelete()) {
            this.resources.delete();
            this.resources = null;
            this.clearAllCachedBatches();
        }
    }

    public ChunkRenderList getRenderList() {
        return this.renderList;
    }

    public int getOrAcquireId(IntPool pool) {
        if (this.uniqueId == -1) {
            this.uniqueId = pool.acquire();
        }
        return this.uniqueId;
    }

    public int getId() {
        return this.uniqueId;
    }

    private final RegionAllocatorHandle.AllocationChangeConsumer geometryChangeConsumer = new RegionAllocatorHandle.AllocationChangeConsumer() {
        @Override
        public void onBufferChanged() {
            RenderRegion.this.onGeometryBufferChange();
        }

        @Override
        public void onSegmentChanged(int ownerIndex) {
            RenderRegion.this.onGeometrySegmentChange(ownerIndex);
        }
    };

    private final RegionAllocatorHandle.AllocationChangeConsumer indexChangeConsumer = new RegionAllocatorHandle.AllocationChangeConsumer() {
        @Override
        public void onBufferChanged() {
            RenderRegion.this.onIndexBufferChange();
        }

        @Override
        public void onSegmentChanged(int ownerIndex) {
            RenderRegion.this.onIndexSegmentChange(ownerIndex);
        }
    };

    public static class DeviceResources {
        private final RegionAllocatorHandle geometryHandle;
        private final RegionAllocatorHandle indexHandle;

        /**
         * The buffer arenas return offsets in terms of how many stride units big things
         * are. This means that if the stride is the length of a vertex, the buffer
         * arena works with vertices and returns vertex offsets. The arena working with
         * indices has as stride of four corresponding to the length of an integer. The
         * two can't easily be combined because integers and vertices require different
         * amounts of data which makes the returned offsets incompatible.
         */
        public DeviceResources(RenderRegion region) {
            int stride = ChunkMeshFormats.getCurrent().getVertexFormat().getVertexSize();

            this.geometryHandle = region.arenaAggregator.getGeometryBufferAllocator(region, stride, region.geometryChangeConsumer);
            this.indexHandle = region.arenaAggregator.getIndexBufferAllocator(region, Integer.BYTES, region.indexChangeConsumer);
        }

        public GpuBuffer getGeometryBuffer() {
            return this.geometryHandle.getBufferObject();
        }

        public GpuBuffer getIndexBuffer() {
            return this.indexHandle.getBufferObject();
        }

        public void delete() {
            this.geometryHandle.deleteSingleOwner();
            this.indexHandle.deleteSingleOwner();
        }

        public RegionAllocatorHandle getGeometryAllocator() {
            return this.geometryHandle;
        }

        public RegionAllocatorHandle getIndexAllocator() {
            return this.indexHandle;
        }

        public boolean shouldDelete() {
            return this.geometryHandle.isEmpty() && this.indexHandle.isEmpty();
        }
    }
}
