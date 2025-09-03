package net.caffeinemc.mods.sodium.mixin.frapi;

import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockModelPart;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(BlockModelPart.class)
public interface BlockModelPartMixin extends FabricBlockModelPart {
    @Override
    default void emitQuads(QuadEmitter emitter, Predicate<@Nullable Direction> cullTest) {
        if (emitter instanceof AbstractBlockRenderContext.BlockEmitter be) {
            be.emitPart((BlockModelPart) this, cullTest, SodiumRenderer.BUFFERER);
        } else {
            FabricBlockModelPart.super.emitQuads(emitter, cullTest);
        }
    }
}
