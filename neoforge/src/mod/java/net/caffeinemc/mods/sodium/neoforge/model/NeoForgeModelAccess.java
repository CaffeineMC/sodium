package net.caffeinemc.mods.sodium.neoforge.model;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
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
import net.neoforged.neoforge.model.data.ModelData;

import java.util.List;
import java.util.Set;

public class NeoForgeModelAccess implements PlatformModelAccess {
    @Override
    public List<BakedQuad> getQuads(BlockAndTintGetter level, BlockPos pos, BlockModelPart model, BlockState state, Direction face, RandomSource random, ChunkSectionLayer renderType) {
        return model.getQuads(face);
    }

    @Override
    public SodiumModelDataContainer getModelDataContainer(Level level, SectionPos sectionPos) {
        Set<Long2ObjectMap.Entry<ModelData>> entrySet = level.getModelDataManager().getAt(sectionPos).long2ObjectEntrySet();
        Long2ObjectMap<SodiumModelData> modelDataMap = new Long2ObjectOpenHashMap<>(entrySet.size());

        for (Long2ObjectMap.Entry<ModelData> entry : entrySet) {
            modelDataMap.put(entry.getLongKey(), (SodiumModelData) (Object) entry.getValue());
        }

        return new SodiumModelDataContainer(modelDataMap);
    }

    @Override
    public List<BlockModelPart> collectPartsOf(BlockStateModel blockStateModel, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, QuadEmitter emitter) {
        if (emitter instanceof AbstractBlockRenderContext.BlockEmitter be) {
            be.cachedList().clear();
            blockStateModel.collectParts(blockView, pos, state, random, be.cachedList());
            return be.cachedList();
        } else {
            return blockStateModel.collectParts(blockView, pos, state, random);
        }
    }

    @Override
    public SodiumModelData getEmptyModelData() {
        return (SodiumModelData) (Object) ModelData.EMPTY;
    }

    @Override
    public ChunkSectionLayer getPartRenderType(BlockModelPart part, BlockState state, ChunkSectionLayer defaultType) {
        return part.getRenderType(state);
    }
}
