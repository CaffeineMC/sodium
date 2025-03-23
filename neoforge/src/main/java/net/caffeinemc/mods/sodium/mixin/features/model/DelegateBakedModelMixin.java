package net.caffeinemc.mods.sodium.mixin.features.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(value = DelegateBlockStateModel.class, priority = 1010)
public class DelegateBakedModelMixin implements FabricBlockStateModel {
    @Shadow
    @Final
    protected BlockStateModel delegate;

   /* @Override
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
        if (!((FabricBakedModel) this.delegate).isVanillaAdapter()) {
            ((FabricBakedModel) this.delegate).emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
        } else {
            VanillaModelEncoder.emitBlockQuads(emitter, (BlockStateModel)this, state, randomSupplier, cullTest);
        }
    }*/
}
