package net.caffeinemc.mods.sodium.mixin.features.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.DelegateBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(value = DelegateBakedModel.class, priority = 1010)
public class DelegateBakedModelMixin implements FabricBakedModel {
    @Shadow
    @Final
    protected BakedModel parent;

    @Override
    public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
        if (!((FabricBakedModel) this.parent).isVanillaAdapter()) {
            ((FabricBakedModel) this.parent).emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
        } else {
            VanillaModelEncoder.emitBlockQuads(emitter, (BakedModel)this, state, randomSupplier, cullTest);
        }
    }

    @Override
    public void emitItemQuads(QuadEmitter emitter, Supplier<RandomSource> randomSupplier) {
        if (!((FabricBakedModel) this.parent).isVanillaAdapter()) {
            ((FabricBakedModel) this.parent).emitItemQuads(emitter, randomSupplier);
        } else {
            VanillaModelEncoder.emitItemQuads(emitter, ((BakedModel) this), null, randomSupplier);
        }
    }
}
