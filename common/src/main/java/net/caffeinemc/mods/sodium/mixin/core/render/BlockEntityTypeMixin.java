package net.caffeinemc.mods.sodium.mixin.core.render;

import net.caffeinemc.mods.sodium.api.blockentity.BlockEntityRenderPredicate;
import net.caffeinemc.mods.sodium.client.render.chunk.ExtendedBlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntityType.class)
public class BlockEntityTypeMixin<T extends BlockEntity> implements ExtendedBlockEntityType<T> {
    @Unique
    private BlockEntityRenderPredicate<T>[] sodium$renderPredicates = new BlockEntityRenderPredicate[0];

    @Override
    public BlockEntityRenderPredicate<T>[] sodium$getRenderPredicates() {
        return this.sodium$renderPredicates;
    }

    @Override
    public void sodium$addRenderPredicate(BlockEntityRenderPredicate<T> predicate) {
        this.sodium$renderPredicates = ArrayUtils.add(this.sodium$renderPredicates, predicate);
    }

    @Override
    public boolean sodium$removeRenderPredicate(BlockEntityRenderPredicate<T> predicate) {
        int index = ArrayUtils.indexOf(this.sodium$renderPredicates, predicate);

        if (index == ArrayUtils.INDEX_NOT_FOUND) {
            return false;
        }

        this.sodium$renderPredicates = ArrayUtils.remove(this.sodium$renderPredicates, index);
        return true;
    }
}
