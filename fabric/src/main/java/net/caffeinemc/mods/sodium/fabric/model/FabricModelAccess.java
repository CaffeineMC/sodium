package net.caffeinemc.mods.sodium.fabric.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.caffeinemc.mods.sodium.client.render.helper.ListStorage;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class FabricModelAccess implements PlatformModelAccess {
    private static final SodiumModelDataContainer EMPTY_CONTAINER = new SodiumModelDataContainer(Long2ObjectMaps.emptyMap());

    @Override
    public List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BlockStateModelPart model, BlockState state, Direction face, RandomSource random) {
        return model.getQuads(face);
    }

    @Override
    public SodiumModelDataContainer getModelDataContainer(Level level, SectionPos sectionPos) {
        return EMPTY_CONTAINER;
    }

    @Override
    public SodiumModelData getEmptyModelData() {
        return null;
    }

    @Override
    public List<BlockStateModelPart> collectPartsOf(BlockStateModel blockStateModel, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, ListStorage emitter) {
        List<BlockStateModelPart> parts = emitter == null ? new ArrayList<>() : emitter.clearAndGet();
        blockStateModel.collectParts(random, parts);
        return parts;
    }
}
