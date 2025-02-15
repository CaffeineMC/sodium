package net.caffeinemc.mods.sodium.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

@Mixin(MultiPartBakedModel.class)
public class MultiPartBakedModelMixin {
    @Unique
    private final Map<BlockState, BlockStateModel[]> stateCacheFast = new Reference2ReferenceOpenHashMap<>();
    @Unique
    private final StampedLock lock = new StampedLock();

    @Shadow
    @Final
    private List<MultiPartBakedModel.Selector> selectors;

    /**
     * @author JellySquid
     * @reason Avoid expensive allocations and replace bitfield indirection
     */
    @Overwrite
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random) {
        if (state == null) {
            return Collections.emptyList();
        }

        BlockStateModel[] models;

        long readStamp = this.lock.readLock();
        try {
            models = this.stateCacheFast.get(state);
        } finally {
            this.lock.unlockRead(readStamp);
        }

        if (models == null) {
            long writeStamp = this.lock.writeLock();
            try {
                List<BlockStateModel> modelList = new ArrayList<>(this.selectors.size());

                for (MultiPartBakedModel.Selector selector : this.selectors) {
                    if (selector.condition().test(state)) {
                        modelList.add(selector.model());
                    }
                }

                models = modelList.toArray(BlockStateModel[]::new);
                this.stateCacheFast.put(state, models);
            } finally {
                this.lock.unlockWrite(writeStamp);
            }
        }

        List<BakedQuad> quads = new ArrayList<>();
        long seed = random.nextLong();

        for (BlockStateModel model : models) {
            random.setSeed(seed);
            quads.addAll(model.getQuads(state, face, random));
        }

        return quads;
    }
}
