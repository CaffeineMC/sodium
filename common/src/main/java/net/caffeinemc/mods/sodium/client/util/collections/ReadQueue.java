package net.caffeinemc.mods.sodium.client.util.collections;

import org.jspecify.annotations.Nullable;

public interface ReadQueue<E> {
    @Nullable E dequeue();
}
