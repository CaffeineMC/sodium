package net.caffeinemc.mods.sodium.client.render.frapi;

import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class FRAPIEmitter implements PlatformModelEmitter {
    @Override
    public void emitModel(BlockStateModel model, Predicate<Direction> cullTest, MutableQuadViewImpl quad, RandomSource random, BlockAndTintGetter blockView, BlockPos pos, BlockState state, Bufferer defaultBuffer) {
        ((FabricBlockStateModel) model).emitQuads(((ExtendedMutableQuadViewImpl) quad).getWrapper(), blockView, pos, state, random, cullTest);
    }
}
