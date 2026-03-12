package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class DefaultModelEmitter implements PlatformModelEmitter {
    @Override
    public void emitModel(BlockStateModel model, Predicate<Direction> cullTest, MutableQuadViewImpl quad, RandomSource random, BlockAndTintGetter blockView, BlockPos pos, BlockState state, Bufferer defaultBuffer) {
        List<BlockStateModelPart> parts = PlatformModelAccess.getInstance().collectPartsOf(model, blockView, pos, state, random, quad);
        for (int i = 0; i < parts.size(); i++) {
            BlockStateModelPart part = parts.get(i);
            defaultBuffer.emit(part, cullTest, MutableQuadViewImpl::emitDirectly);
        }
    }
}
