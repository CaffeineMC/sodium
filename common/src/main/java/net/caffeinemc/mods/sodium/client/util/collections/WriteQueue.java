package net.caffeinemc.mods.sodium.client.util.collections;

import org.jspecify.annotations.NonNull;

public interface WriteQueue<E> {
    void ensureCapacity(int numElements);

    void enqueue(@NonNull E e);
}
