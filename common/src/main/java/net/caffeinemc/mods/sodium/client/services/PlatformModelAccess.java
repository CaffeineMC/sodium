package net.caffeinemc.mods.sodium.client.services;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

public interface PlatformModelAccess {
    PlatformModelAccess INSTANCE = Services.load(PlatformModelAccess.class);

    static PlatformModelAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a list of quads used by this model.
     * @param level The level slice.
     * @param pos The position of the block being rendered.
     * @param model The {@code BakedModel} currently being drawn.
     * @param state The block state of the current block.
     * @param face The current face of the block being rendered, or null if rendering unassigned quads.
     * @param random The random source used by the current block renderer.
     * @param renderType The current render type being drawn.
     * @return The list of quads used by the model.
     */
    List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BlockModelPart model, BlockState state, Direction face, RandomSource random, ChunkSectionLayer renderType);

    /**
     * Gets the container holding model data for this chunk. <b>This operation is not thread safe.</b>
     * @param level The current vanilla Level.
     * @param sectionPos The current chunk position.
     * @return The model data container for this section
     */
    SodiumModelDataContainer getModelDataContainer(Level level, SectionPos sectionPos);

    /**
     * Should not use. <b>Use {@code SodiumModelData.EMPTY} instead.</b>
     * @return The empty model data for this platform.
     */
    @ApiStatus.Internal
    SodiumModelData getEmptyModelData();

    ChunkSectionLayer getPartRenderType(BlockModelPart part, BlockState state, ChunkSectionLayer defaultType);

    List<BlockModelPart> collectPartsOf(BlockStateModel blockStateModel, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, QuadEmitter emitter);
}
