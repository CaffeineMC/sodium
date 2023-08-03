package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMaps;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPaletteFallback;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalleteArray;
import me.jellysquid.mods.sodium.mixin.core.world.chunk.PalettedContainerAccessor;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ClonedChunkSection {
    private static final ChunkNibbleArray DEFAULT_SKY_LIGHT_ARRAY = new ChunkNibbleArray(15);
    private static final ChunkNibbleArray DEFAULT_BLOCK_LIGHT_ARRAY = new ChunkNibbleArray(0);

    private final ChunkSectionPos pos;

    private @Nullable Int2ReferenceMap<BlockEntity> blockEntities;
    private @Nullable Int2ReferenceMap<Object> blockEntityAttachments;

    private final @Nullable ChunkNibbleArray[] lightDataArrays = new ChunkNibbleArray[LightType.values().length];

    private @Nullable PackedIntegerArray blockStateData;
    private @Nullable ClonedPalette<BlockState> blockStatePalette;

    private @Nullable ReadableContainer<RegistryEntry<Biome>> biomeData;

    private long lastUsedTimestamp = Long.MAX_VALUE;

    public ClonedChunkSection(World world, WorldChunk chunk, @Nullable ChunkSection section, ChunkSectionPos pos) {
        this.pos = pos;

        if (section != null) {
            if (!section.isEmpty()) {
                this.copyBlockData(section);
                this.copyBlockEntities(chunk, pos);
            }
            this.copyBiomeData(section);
        }

        this.copyLightData(world);
    }

    private void copyBlockData(ChunkSection section) {
        PalettedContainer.Data<BlockState> container = ((PalettedContainerAccessor<BlockState>) section.getBlockStateContainer()).getData();

        this.blockStateData = copyBlockData(container);
        this.blockStatePalette = copyPalette(container);
    }

    private void copyLightData(World world) {
        this.lightDataArrays[LightType.BLOCK.ordinal()] = copyLightArray(world, LightType.BLOCK, this.pos);

        // Dimensions without sky-light should not have a default-initialized array
        if (world.getDimension().hasSkyLight()) {
            this.lightDataArrays[LightType.SKY.ordinal()] = copyLightArray(world, LightType.SKY, this.pos);
        }
    }

    /**
     * Copies the light data array for the given light type for this chunk, or returns a default-initialized value if
     * the light array is not loaded.
     */
    private static ChunkNibbleArray copyLightArray(World world, LightType type, ChunkSectionPos pos) {
        var array = world.getLightingProvider()
                .get(type)
                .getLightSection(pos);

        if (array == null) {
            array = switch (type) {
                case SKY -> DEFAULT_SKY_LIGHT_ARRAY;
                case BLOCK -> DEFAULT_BLOCK_LIGHT_ARRAY;
            };
        }

        return array;
    }

    private void copyBiomeData(ChunkSection section) {
        this.biomeData = section.getBiomeContainer();
    }

    private void copyBlockEntities(WorldChunk chunk, ChunkSectionPos chunkCoord) {
        BlockBox box = new BlockBox(chunkCoord.getMinX(), chunkCoord.getMinY(), chunkCoord.getMinZ(),
                chunkCoord.getMaxX(), chunkCoord.getMaxY(), chunkCoord.getMaxZ());

        Int2ReferenceOpenHashMap<BlockEntity> blockEntities = null;

        // Copy the block entities from the chunk into our cloned section
        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockEntity entity = entry.getValue();

            if (box.contains(pos)) {
                if (blockEntities == null) {
                    blockEntities = new Int2ReferenceOpenHashMap<>();
                }

                blockEntities.put(WorldSlice.getLocalBlockIndex(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15), entity);
            }
        }

        this.blockEntities = blockEntities != null ? blockEntities : Int2ReferenceMaps.emptyMap();

        Int2ReferenceOpenHashMap<Object> blockEntityAttachments = null;

        // Retrieve any render attachments after we have copied all block entities, as this will call into the code of
        // other mods. This could potentially result in the chunk being modified, which would cause problems if we
        // were iterating over any data in that chunk.
        // See https://github.com/CaffeineMC/sodium-fabric/issues/942 for more info.
        for (var entry : Int2ReferenceMaps.fastIterable(this.blockEntities)) {
            if (entry.getValue() instanceof RenderAttachmentBlockEntity holder) {
                if (blockEntityAttachments == null) {
                    blockEntityAttachments = new Int2ReferenceOpenHashMap<>();
                }

                blockEntityAttachments.put(entry.getIntKey(), holder.getRenderAttachmentData());
            }
        }

        this.blockEntityAttachments = blockEntityAttachments != null ? blockEntityAttachments : Int2ReferenceMaps.emptyMap();
    }

    public @Nullable ReadableContainer<RegistryEntry<Biome>> getBiomeData() {
        return this.biomeData;
    }

    public @Nullable PackedIntegerArray getBlockData() {
        return this.blockStateData;
    }

    public @Nullable ClonedPalette<BlockState> getBlockPalette() {
        return this.blockStatePalette;
    }

    public ChunkSectionPos getPosition() {
        return this.pos;
    }

    private static ClonedPalette<BlockState> copyPalette(PalettedContainer.Data<BlockState> container) {
        Palette<BlockState> palette = container.palette();

        if (palette instanceof IdListPalette) {
            return new ClonedPaletteFallback<>(Block.STATE_IDS);
        }

        BlockState[] array = new BlockState[container.palette().getSize()];

        for (int i = 0; i < array.length; i++) {
            array[i] = palette.get(i);
        }

        return new ClonedPalleteArray<>(array);
    }

    private static PackedIntegerArray copyBlockData(PalettedContainer.Data<BlockState> container) {
        var storage = container.storage();
        var data = storage.getData();
        var bits = container.configuration().bits();

        if (bits == 0) {
            return null;
        }

        return new PackedIntegerArray(bits, storage.getSize(), data.clone());
    }

    public long getLastUsedTimestamp() {
        return this.lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long timestamp) {
        this.lastUsedTimestamp = timestamp;
    }

    public @Nullable Int2ReferenceMap<BlockEntity> getBlockEntityMap() {
        return this.blockEntities;
    }

    public @Nullable Int2ReferenceMap<Object> getBlockEntityAttachmentMap() {
        return this.blockEntityAttachments;
    }

    public @Nullable ChunkNibbleArray getLightArray(LightType lightType) {
        return this.lightDataArrays[lightType.ordinal()];
    }
}
