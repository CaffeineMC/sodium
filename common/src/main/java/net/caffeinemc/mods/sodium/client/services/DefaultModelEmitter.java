package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class DefaultModelEmitter implements PlatformModelEmitter {
    @Override
    public void emitModel(BlockStateModel model, Predicate<Direction> cullTest, MutableQuadViewImpl quad, RandomSource random, BlockAndTintGetter blockView, BlockPos pos, BlockState state, Bufferer defaultBuffer) {
        List<BlockModelPart> parts = PlatformModelAccess.getInstance().collectPartsOf(model, blockView, pos, state, random, quad);

        if (quad instanceof AbstractBlockRenderContext.BlockEmitter emitter) {
            ChunkSectionLayer type = ItemBlockRenderTypes.getChunkRenderType(state);

            for (int i = 0; i < parts.size(); ++i) {
                if (PlatformModelAccess.getInstance().getPartRenderType(parts.get(i), state, type) != type) {
                    emitter.markInvalidToDowngrade();
                    break;
                }
            }
        }

        for (int i = 0; i < parts.size(); i++) {
            BlockModelPart part = parts.get(i);
            defaultBuffer.emit(part, cullTest, MutableQuadViewImpl::emitDirectly);
        }
    }
}
