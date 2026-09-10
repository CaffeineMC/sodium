package net.caffeinemc.mods.sodium.client.model.color;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public final class DefaultColorProviders {
    public static ColorProvider<BlockState> adapt(BlockTintSource color) {
        return new VanillaAdapter(color);
    }

    public static ColorProvider<BlockState> adapt(BlockTintSource[] colors) {
        return new VanillaAdapter(colors);
    }

    public static class GrassColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new GrassColorProvider<>();

        private GrassColorProvider() {}

        @Override
        protected int getColor(LevelSlice slice, T state, BlockPos pos) {
            return 0xFF000000 | BiomeColors.getAverageGrassColor(slice, pos);
        }
    }

    public static class FoliageColorProvider<T> extends BlendedColorProvider<T> {
        public static final ColorProvider<BlockState> BLOCKS = new FoliageColorProvider<>();

        private FoliageColorProvider() {}

        @Override
        protected int getColor(LevelSlice slice, T state, BlockPos pos) {
            return 0xFF000000 | BiomeColors.getAverageFoliageColor(slice, pos);
        }
    }

    private static class VanillaAdapter implements ColorProvider<BlockState> {
        private final BlockTintSource[] sources;

        private VanillaAdapter(BlockTintSource source) {
            this.sources = new BlockTintSource[] { source };
        }

        public VanillaAdapter(BlockTintSource[] sources) {
            this.sources = sources;
        }

        @Override
        public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, BlockState state, ModelQuadView quad, int[] output, boolean smooth) {
            if (quad.getTintIndex() >= this.sources.length) {
                Arrays.fill(output, -1);
                return;
            }

            Arrays.fill(output, 0xFF000000 | this.sources[quad.getTintIndex()].colorInWorld(state, slice, pos));
        }
    }
}
