package net.caffeinemc.mods.sodium.mixin.frapi;

import net.caffeinemc.mods.sodium.client.render.helper.ListStorage;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockModelPart;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Predicate;

@Mixin(BlockStateModel.class)
public interface BakedModelMixin extends FabricBlockStateModel {
    @Override
    default void emitQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
        List<BlockModelPart> parts = PlatformModelAccess.getInstance().collectPartsOf((BlockStateModel) this, blockView, pos, state, random, emitter instanceof ListStorage ls ? ls : null);
        int partCount = parts.size();

        if (emitter instanceof AbstractBlockRenderContext.BlockEmitter be) {
            ChunkSectionLayer type = ItemBlockRenderTypes.getChunkRenderType(state);

            for (int i = 0; i < partCount; ++i) {
                if (PlatformModelAccess.getInstance().getPartRenderType(parts.get(i), state, type) != type) {
                    be.markInvalidToDowngrade();
                    break;
                }
            }
        }

        for (int i = 0; i < partCount; ++i) {
            ((FabricBlockModelPart) parts.get(i)).emitQuads(emitter, cullTest);
        }
    }
}
