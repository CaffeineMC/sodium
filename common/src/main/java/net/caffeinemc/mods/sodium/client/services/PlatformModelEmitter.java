package net.caffeinemc.mods.sodium.client.services;

import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.util.TriConsumer;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface PlatformModelEmitter {
    PlatformModelEmitter INSTANCE = Services.loadOr(PlatformModelEmitter.class, DefaultModelEmitter::new);

    static PlatformModelEmitter getInstance() {
        return INSTANCE;
    }

    void emitModel(BlockStateModel model, Predicate<Direction> cullTest, MutableQuadViewImpl quad, RandomSource random, BlockAndTintGetter blockView, BlockPos pos, BlockState state, Bufferer defaultBuffer);

    @FunctionalInterface
    public interface Bufferer {
        void emit(BlockModelPart part, Predicate<Direction> cullTest, Consumer<MutableQuadViewImpl> emitter);
    }
}
