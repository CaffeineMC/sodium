package net.caffeinemc.mods.sodium.neoforge.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import java.util.Arrays;

public class NeoForgeDynamicColorProvider implements ColorProvider<BlockState> {
    private final IntList list = new IntArrayList();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private BlockState state;
    private LevelSlice slice;

    @Override
    public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, BlockState state, ModelQuadView quad, int[] output, boolean smooth) {
        int tintIndex = quad.getTintIndex();

        if (this.slice != slice || this.state != state || !this.pos.equals(pos)) {
            this.pos.set(pos);
            this.slice = slice;
            this.state = state;

            this.list.clear();
            IClientBlockExtensions.of(state).collectDynamicTintValues(state, slice, pos, this.list);
        }

        if (tintIndex < 0 || tintIndex >= this.list.size()) {
            Arrays.fill(output, 0xFFFFFFFF);
            return;
        }

        Arrays.fill(output, this.list.getInt(tintIndex));
    }
}
