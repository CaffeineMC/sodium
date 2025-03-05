package net.caffeinemc.mods.sodium.client.world;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import net.caffeinemc.mods.sodium.client.services.*;
import net.caffeinemc.mods.sodium.client.util.collections.NibbleArray;
import net.caffeinemc.mods.sodium.client.world.biome.LevelColorCache;
import net.caffeinemc.mods.sodium.client.world.biome.LevelBiomeSlice;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSection;
import net.caffeinemc.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * <p>Takes a slice of level state (block states, biome and light data arrays) and copies the data for use in off-thread
 * operations. This allows chunk build tasks to see a consistent snapshot of chunk data at the exact moment the task was
 * created.</p>
 *
 * <p>World slices are not safe to use from multiple threads at once, but the data they contain is safe from modification
 * by the main client thread.</p>
 *
 * <p>Object pooling should be used to avoid huge allocations as this class contains many large arrays.</p>
 */
public final class LevelSlice implements BlockAndTintGetter, RenderAttachedBlockView, FabricBlockView {
    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = 1;

    // The number of sections on each axis of this slice.
    private static final int SECTION_ARRAY_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the (Local Section -> Resource) arrays.
    private static final int SECTION_ARRAY_SIZE = SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH;

    // The number of bits needed for each local X/Y/Z coordinate.
    private static final int LOCAL_XYZ_BITS = 4;

    // The default block state used for out-of-bounds access
    private static final BlockState EMPTY_BLOCK_STATE = Blocks.AIR.defaultBlockState();

    // The level this slice has copied data from
    private final ClientLevel level;

    // The accessor used for fetching biome data from the slice
    private final LevelBiomeSlice biomeSlice;

    // The biome blend cache
    private final LevelColorCache biomeColors;

    // (Local Section -> Block States) table.
    private final BlockState[][] blockArrays;

    // (Local Section -> Light Manager) table.
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    private final SodiumAuxiliaryLightManager[] auxLightManager;

    // (Local Section -> Light Arrays) table.
    private final byte[][] skyLightArrays;
    private final byte[][] blockLightArrays;

    // (Local Section -> Block Entity) table.
    private final @Nullable Int2ReferenceMap<BlockEntity>[] blockEntityArrays;

    // (Local Section -> Block Entity Render Data) table.
    private final @Nullable Int2ReferenceMap<Object>[] blockEntityRenderDataArrays;

    // (Local Section -> Model Data) table.
    private final SodiumModelDataContainer[] modelMapArrays;

    // The starting point from which this slice captures blocks
    private int originBlockX, originBlockY, originBlockZ;

    public static ChunkRenderContext prepare(Level level, SectionPos pos, ClonedChunkSectionCache cache) {
        LevelChunk chunk = level.getChunk(pos.getX(), pos.getZ());
        LevelChunkSection section = chunk.getSections()[level.getSectionIndexFromSectionY(pos.getY())];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't be created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the level can be loaded.
        if (section == null || section.hasOnlyAir()) {
            return null;
        }

        // The min/max bounds of the chunks copied by this slice
        final int minChunkX = pos.getX() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkY = pos.getY() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkZ = pos.getZ() - NEIGHBOR_CHUNK_RADIUS;

        final int maxChunkX = pos.getX() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkY = pos.getY() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkZ = pos.getZ() + NEIGHBOR_CHUNK_RADIUS;

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_ARRAY_SIZE];

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    sections[getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ)] =
                            cache.acquire(chunkX, chunkY, chunkZ);
                }
            }
        }

        List<?> renderers = PlatformLevelRenderHooks.getInstance().retrieveChunkMeshAppenders(level, pos.origin());

        return new ChunkRenderContext(pos, sections, renderers);
    }

    @SuppressWarnings("unchecked")
    public LevelSlice(ClientLevel level) {
        this.level = level;

        this.blockArrays = new BlockState[SECTION_ARRAY_SIZE][];

        this.skyLightArrays = new byte[SECTION_ARRAY_SIZE][];
        this.blockLightArrays = new byte[SECTION_ARRAY_SIZE][];

        this.blockEntityArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];
        this.blockEntityRenderDataArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];
        this.auxLightManager = new SodiumAuxiliaryLightManager[SECTION_ARRAY_SIZE];
        this.modelMapArrays = new SodiumModelDataContainer[SECTION_ARRAY_SIZE];

        this.biomeSlice = new LevelBiomeSlice();
        this.biomeColors = new LevelColorCache(this.biomeSlice, Minecraft.getInstance().options.biomeBlendRadius().get());
    }

    public void copyData(ChunkRenderContext context) {
        this.originBlockX = SectionPos.sectionToBlockCoord(context.getOrigin().getX() - NEIGHBOR_CHUNK_RADIUS);
        this.originBlockY = SectionPos.sectionToBlockCoord(context.getOrigin().getY() - NEIGHBOR_CHUNK_RADIUS);
        this.originBlockZ = SectionPos.sectionToBlockCoord(context.getOrigin().getZ() - NEIGHBOR_CHUNK_RADIUS);

        for (int x = 0; x < SECTION_ARRAY_LENGTH; x++) {
            for (int y = 0; y < SECTION_ARRAY_LENGTH; y++) {
                for (int z = 0; z < SECTION_ARRAY_LENGTH; z++) {
                    this.copySectionData(context, getLocalSectionIndex(x, y, z));
                }
            }
        }

        this.biomeSlice.update(this.level, context);
        this.biomeColors.update(context);
    }

    private void copySectionData(ChunkRenderContext context, int sectionIndex) {
        var section = context.getSections()[sectionIndex];

        Objects.requireNonNull(section, "Chunk section must be non-null");

        this.blockArrays[sectionIndex] = section.getBlockData();

        this.skyLightArrays[sectionIndex] = section.getSkyLightArray();
        this.blockLightArrays[sectionIndex] = section.getBlockLightArray();

        this.blockEntityArrays[sectionIndex] = section.getBlockEntityMap();
        this.auxLightManager[sectionIndex] = section.getAuxLightManager();
        this.blockEntityRenderDataArrays[sectionIndex] = section.getBlockEntityRenderDataMap();
        this.modelMapArrays[sectionIndex] = section.getModelMap();
    }

    public void reset() {
        // erase any pointers to resources we no longer need
        // no point in cleaning the pre-allocated arrays (such as block state storage) since we hold the
        // only reference.
        for (int sectionIndex = 0; sectionIndex < SECTION_ARRAY_LENGTH; sectionIndex++) {
            this.blockArrays[sectionIndex] = null;

            this.skyLightArrays[sectionIndex] = null;
            this.blockLightArrays[sectionIndex] = null;

            this.blockEntityArrays[sectionIndex] = null;
            this.auxLightManager[sectionIndex] = null;
            this.blockEntityRenderDataArrays[sectionIndex] = null;
        }
    }

    @Override
    public @NotNull BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int blockX, int blockY, int blockZ) {
        int relBlockX = blockX - this.originBlockX;
        int relBlockY = blockY - this.originBlockY;
        int relBlockZ = blockZ - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return EMPTY_BLOCK_STATE;
        }

        return this.blockArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)]
                [getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15)];
    }

    @Override
    public @NotNull FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos)
                .getFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return this.level.getShade(direction, shaded);
    }

    @Override
    public @NotNull LevelLightEngine getLightEngine() {
        // Not thread-safe to access lighting data from off-thread, even if Minecraft allows it.
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return 0;
        }

        int localSectionIndex = getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4);
        int localBlockIndex = getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15);

        byte[] lightArray = this.getLightArray(localSectionIndex, type);

        return NibbleArray.get(lightArray, localBlockIndex);
    }

    private byte[] getLightArray(int localSectionIndex, LightLayer type) {
        byte[][] arrays = switch (type) {
            case SKY -> this.skyLightArrays;
            case BLOCK -> this.blockLightArrays;
        };

        return arrays[localSectionIndex];
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarkness) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return 0;
        }

        int localSectionIndex = getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4);
        int localBlockIndex = getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15);

        var skyLightArray = this.skyLightArrays[localSectionIndex];
        var blockLightArray = this.blockLightArrays[localSectionIndex];

        int skyLight = NibbleArray.get(skyLightArray, localBlockIndex) - ambientDarkness;
        int blockLight = NibbleArray.get(blockLightArray, localBlockIndex);

        return Math.max(blockLight, skyLight);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public @Nullable BlockEntity getBlockEntity(int blockX, int blockY, int blockZ) {
        int relBlockX = blockX - this.originBlockX;
        int relBlockY = blockY - this.originBlockY;
        int relBlockZ = blockZ - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return null;
        }

        var blockEntities = this.blockEntityArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];

        if (blockEntities == null) {
            return null;
        }

        return blockEntities.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return this.biomeColors.getColor(resolver, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getHeight() {
        return this.level.getHeight();
    }

    @Override
    public int getMinY() {
        return this.level.getMinY();
    }

    @Override
    public @Nullable Object getBlockEntityRenderData(BlockPos pos) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return null;
        }

        var blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];

        if (blockEntityRenderDataMap == null) {
            return null;
        }

        return blockEntityRenderDataMap.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
    }

    public SodiumModelData getPlatformModelData(BlockPos pos) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return SodiumModelData.EMPTY;
        }

        var modelMap = this.modelMapArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];

        if (modelMap.isEmpty()) {
            return SodiumModelData.EMPTY;
        }

        return modelMap.getModelData(pos);
    }

    @Override
    public boolean hasBiomes() {
        return true;
    }

    @Override
    public Holder<Biome> getBiomeFabric(BlockPos pos) {
        return this.biomeSlice.getBiome(pos.getX(), pos.getY(), pos.getZ());
    }

    public static int getLocalBlockIndex(int blockX, int blockY, int blockZ) {
        return (blockY << LOCAL_XYZ_BITS << LOCAL_XYZ_BITS) | (blockZ << LOCAL_XYZ_BITS) | blockX;
    }

    public static int getLocalSectionIndex(int sectionX, int sectionY, int sectionZ) {
        return (sectionY * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH) + (sectionZ * SECTION_ARRAY_LENGTH) + sectionX;
    }

    @Override
    public @Nullable Object getBlockEntityRenderAttachment(BlockPos pos) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        if (!isLocalBlockCoordWithinBounds(relBlockX, relBlockY, relBlockZ)) {
            return null;
        }

        var blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];

        if (blockEntityRenderDataMap == null) {
            return null;
        }

        return blockEntityRenderDataMap.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
    }

    private static boolean isLocalBlockCoordWithinBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < 48 && y < 48 && z < 48;
    }
}
