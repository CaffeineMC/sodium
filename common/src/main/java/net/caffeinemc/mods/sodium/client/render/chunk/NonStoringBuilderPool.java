package net.caffeinemc.mods.sodium.client.render.chunk;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class NonStoringBuilderPool extends SectionBufferBuilderPool {
    public NonStoringBuilderPool() {
        var pack = new SectionBufferBuilderPack();
        pack.close();

        super(List.of(pack));
    }

    @Nullable
    @Override
    public SectionBufferBuilderPack acquire() {
        return null;
    }

    @Override
    public void release(SectionBufferBuilderPack blockBufferBuilderStorage) {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int getFreeBufferCount() {
        return 0;
    }
}
