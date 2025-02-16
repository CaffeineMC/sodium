package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import net.caffeinemc.mods.sodium.client.render.chunk.LocalSectionIndex;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionFlags;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteArrayIterator;
import net.caffeinemc.mods.sodium.client.util.iterator.ByteIterator;
import net.caffeinemc.mods.sodium.client.util.iterator.ReversibleByteArrayIterator;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ChunkRenderList {
    private final RenderRegion region;

    private final byte[] sectionsWithGeometry = new byte[RenderRegion.REGION_SIZE];
    private final long[] sectionsWithGeometryMap = new long[RenderRegion.REGION_SIZE / Long.SIZE];
    private final long[] prevSectionsWithGeometryMap = new long[RenderRegion.REGION_SIZE / Long.SIZE];
    private int sectionsWithGeometryCount = 0;
    private int prevSectionsWithGeometryCount = 0;
    private int lastRelativeCameraSectionX;
    private int lastRelativeCameraSectionY;
    private int lastRelativeCameraSectionZ;

    private final byte[] sectionsWithSprites = new byte[RenderRegion.REGION_SIZE];
    private int sectionsWithSpritesCount = 0;

    private final byte[] sectionsWithEntities = new byte[RenderRegion.REGION_SIZE];
    private int sectionsWithEntitiesCount = 0;

    private int size;

    private int lastVisibleFrame;

    public ChunkRenderList(RenderRegion region) {
        this.region = region;
    }

    public void reset(int frame) {
        this.prevSectionsWithGeometryCount = this.sectionsWithGeometryCount;
        Arrays.fill(this.sectionsWithGeometryMap, 0L);

        this.sectionsWithGeometryCount = 0;
        this.sectionsWithSpritesCount = 0;
        this.sectionsWithEntitiesCount = 0;

        this.size = 0;
        this.lastVisibleFrame = frame;
    }

    // clamping the relative camera position to the region bounds means there can only be very few different distances
    private static final int SORTING_HISTOGRAM_SIZE = RenderRegion.REGION_WIDTH + RenderRegion.REGION_HEIGHT + RenderRegion.REGION_LENGTH - 2;

    public void prepareForRender(SectionPos cameraPos, SortItemsProvider sortItemsProvider, boolean sortSections) {
        // The relative coordinates are clamped to one section larger than the region bounds to also capture cache invalidation that happens
        // when the camera moves from outside the region to inside the region (when seen on all axes independently).
        // This type of cache invalidation stems from different facings of sections being rendered if the camera is aligned with them on an axis.
        // For sorting only the position clamped to inside the region is used.
        int relativeCameraSectionX = Mth.clamp(cameraPos.getX() - this.region.getChunkX(), -1, RenderRegion.REGION_WIDTH);
        int relativeCameraSectionY = Mth.clamp(cameraPos.getY() - this.region.getChunkY(), -1, RenderRegion.REGION_HEIGHT);
        int relativeCameraSectionZ = Mth.clamp(cameraPos.getZ() - this.region.getChunkZ(), -1, RenderRegion.REGION_LENGTH);

        // invalidate batch cache if the render list changed
        if (this.prevSectionsWithGeometryCount != this.sectionsWithGeometryCount ||
                relativeCameraSectionX != this.lastRelativeCameraSectionX ||
                relativeCameraSectionY != this.lastRelativeCameraSectionY ||
                relativeCameraSectionZ != this.lastRelativeCameraSectionZ ||
                !Arrays.equals(this.sectionsWithGeometryMap, this.prevSectionsWithGeometryMap)) {
            // reset cache invalidation, the newly built batches will remain valid until the next change
            this.region.clearAllCachedBatches();

            this.prevSectionsWithGeometryCount = this.sectionsWithGeometryCount;
            System.arraycopy(this.sectionsWithGeometryMap, 0, this.prevSectionsWithGeometryMap, 0, this.sectionsWithGeometryMap.length);
            this.lastRelativeCameraSectionX = relativeCameraSectionX;
            this.lastRelativeCameraSectionY = relativeCameraSectionY;
            this.lastRelativeCameraSectionZ = relativeCameraSectionZ;

            // only sort sections if necessary, read directly from bitmap instead of no sorting is required
            if (sortSections) {
                this.sortSections(relativeCameraSectionX, relativeCameraSectionY, relativeCameraSectionZ, sortItemsProvider);
            } else {
                this.readSections();
            }
        }
    }

    private void sortSections(int relativeCameraSectionX, int relativeCameraSectionY, int relativeCameraSectionZ, SortItemsProvider sortItemsProvider) {
        relativeCameraSectionX = Mth.clamp(relativeCameraSectionX, 0, RenderRegion.REGION_WIDTH - 1);
        relativeCameraSectionY = Mth.clamp(relativeCameraSectionY, 0, RenderRegion.REGION_HEIGHT - 1);
        relativeCameraSectionZ = Mth.clamp(relativeCameraSectionZ, 0, RenderRegion.REGION_LENGTH - 1);

        int[] histogram = new int[SORTING_HISTOGRAM_SIZE];
        var sortItems = sortItemsProvider.ensureSortItemsOfLength(this.sectionsWithGeometryCount);

        this.sectionsWithGeometryCount = 0;
        for (int mapIndex = 0; mapIndex < this.sectionsWithGeometryMap.length; mapIndex++) {
            var map = this.sectionsWithGeometryMap[mapIndex];
            var mapOffset = mapIndex << 6;

            while (map != 0) {
                var index = Long.numberOfTrailingZeros(map) + mapOffset;
                map &= map - 1;

                var x = Math.abs(LocalSectionIndex.unpackX(index) - relativeCameraSectionX);
                var y = Math.abs(LocalSectionIndex.unpackY(index) - relativeCameraSectionY);
                var z = Math.abs(LocalSectionIndex.unpackZ(index) - relativeCameraSectionZ);

                var distance = x + y + z;
                histogram[distance]++;
                sortItems[this.sectionsWithGeometryCount++] = distance << 8 | index;
            }
        }

        // prefix sum to calculate indexes
        for (int i = 1; i < SORTING_HISTOGRAM_SIZE; i++) {
            histogram[i] += histogram[i - 1];
        }

        for (int i = 0; i < this.sectionsWithGeometryCount; i++) {
            var item = sortItems[i];
            var distance = item >>> 8;
            this.sectionsWithGeometry[--histogram[distance]] = (byte) item;
        }
    }

    private void readSections() {
        this.sectionsWithGeometryCount = 0;
        for (int mapIndex = 0; mapIndex < this.sectionsWithGeometryMap.length; mapIndex++) {
            var map = this.sectionsWithGeometryMap[mapIndex];
            var mapOffset = mapIndex << 6;

            while (map != 0) {
                var index = Long.numberOfTrailingZeros(map) + mapOffset;
                map &= map - 1;

                this.sectionsWithGeometry[this.sectionsWithGeometryCount++] = (byte) index;
            }
        }
    }

    public void add(int localSectionIndex) {
        if (this.size >= RenderRegion.REGION_SIZE) {
            throw new ArrayIndexOutOfBoundsException("Render list is full");
        }

        this.size++;

        int flags = this.region.getSectionFlags(localSectionIndex);

        if (((flags >>> RenderSectionFlags.HAS_BLOCK_GEOMETRY) & 1) == 1) {
            this.sectionsWithGeometryMap[localSectionIndex >> 6] |= 1L << (localSectionIndex & 0b111111);
            this.sectionsWithGeometryCount++;
        }

        this.sectionsWithSprites[this.sectionsWithSpritesCount] = (byte) localSectionIndex;
        this.sectionsWithSpritesCount += (flags >>> RenderSectionFlags.HAS_ANIMATED_SPRITES) & 1;

        this.sectionsWithEntities[this.sectionsWithEntitiesCount] = (byte) localSectionIndex;
        this.sectionsWithEntitiesCount += (flags >>> RenderSectionFlags.HAS_BLOCK_ENTITIES) & 1;
    }

    public @Nullable ByteIterator sectionsWithGeometryIterator(boolean reverse) {
        if (this.sectionsWithGeometryCount == 0) {
            return null;
        }

        return new ReversibleByteArrayIterator(this.sectionsWithGeometry, this.sectionsWithGeometryCount, reverse);
    }

    public @Nullable ByteIterator sectionsWithSpritesIterator() {
        if (this.sectionsWithSpritesCount == 0) {
            return null;
        }

        return new ByteArrayIterator(this.sectionsWithSprites, this.sectionsWithSpritesCount);
    }

    public @Nullable ByteIterator sectionsWithEntitiesIterator() {
        if (this.sectionsWithEntitiesCount == 0) {
            return null;
        }

        return new ByteArrayIterator(this.sectionsWithEntities, this.sectionsWithEntitiesCount);
    }

    public int getSectionsWithGeometryCount() {
        return this.sectionsWithGeometryCount;
    }

    public int getSectionsWithSpritesCount() {
        return this.sectionsWithSpritesCount;
    }

    public int getSectionsWithEntitiesCount() {
        return this.sectionsWithEntitiesCount;
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public int size() {
        return this.size;
    }
}
