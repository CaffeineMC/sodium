package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.world.level.chunk.PalettedContainerRO;

public interface PalettedContainerROExtension<T> {
    @SuppressWarnings("unchecked")
    static <T> PalettedContainerROExtension<T> of(PalettedContainerRO<T> container) {
        return (PalettedContainerROExtension<T>) container;
    }

    static <T> PalettedContainerRO<T> clone(PalettedContainerRO<T> container) {
        if (container == null) {
            return null;
        }

        return of(container).sodium$copy();
    }

    void sodium$unpack(T[] values);

    PalettedContainerRO<T> sodium$copy();
}
