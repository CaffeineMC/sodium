package net.caffeinemc.mods.sodium.client.render.chunk;

import java.util.BitSet;

public final class IntPool {
    private final BitSet used = new BitSet();

    private int next = 0;

    public int acquire() {
        int id = used.nextClearBit(next);
        used.set(id);
        next = used.nextClearBit(id + 1);

        return id;
    }

    public void release(int id) {
        used.clear(id);

        if (id < next) {
            next = id;
        }
    }

    public void clear() {
        used.clear();
        next = 0;
    }
}