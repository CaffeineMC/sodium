package net.caffeinemc.mods.sodium.fabric.model;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockTintsFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class FabricMutableProvider implements ColorProvider<BlockState> {
    private final IntList list = new IntArrayList();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private BlockState state;
    private LevelSlice slice;

    @Override
    public void getColors(LevelSlice slice, BlockPos pos, BlockPos.MutableBlockPos scratchPos, BlockState state, ModelQuadView quad, int[] output, boolean smooth) {
        int tintIndex = quad.getTintIndex();
        final BlockTintsFactory factory = BlockColorRegistry.getFactory(state);

        if (factory == null) {
            Arrays.fill(output, 0xFFFFFFFF);
            return;
        }

        if (this.slice != slice ||  this.state != state || !this.pos.equals(pos)) {
            this.pos.set(pos);
            this.slice = slice;
            this.state = state;

            this.list.clear();
            factory.collect(state, slice, pos, this.list);
        }

        if (tintIndex < 0 || tintIndex >= this.list.size()) {
            Arrays.fill(output, 0xFFFFFFFF);
            return;
        }

        Arrays.fill(output, this.list.getInt(tintIndex));
    }
}
