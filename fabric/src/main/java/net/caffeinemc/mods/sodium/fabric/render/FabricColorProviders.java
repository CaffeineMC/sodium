package net.caffeinemc.mods.sodium.fabric.render;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public class FabricColorProviders {
    public static ColorProvider<FluidState> adapt(@Nullable BlockTintSource handler) {
        return new FabricFluidAdapter(handler);
    }

    private static class FabricFluidAdapter implements ColorProvider<FluidState> {
        private final @Nullable BlockTintSource handler;

        public FabricFluidAdapter(@Nullable BlockTintSource handler) {
            this.handler = handler;
        }

        @Override
        public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, FluidState state, ModelQuadView quad, int[] output, boolean smooth) {
            if (this.handler == null) {
                Arrays.fill(output, -1);
                return;
            }

            var color = this.handler.colorInWorld(state.createLegacyBlock(), slice, pos);
            color = ColorHelper.makeOpaqueIfTransparent(color);
            Arrays.fill(output, color);
        }
    }
}
