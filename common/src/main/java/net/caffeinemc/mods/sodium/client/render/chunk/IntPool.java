package net.caffeinemc.mods.sodium.client.render.chunk;

import java.util.BitSet;

public final class IntPool {
    private final BitSet used = new BitSet();

    private int next = 0;

    public int acquire() {
        int id = this.used.nextClearBit(this.next);
        this.used.set(id);
        this.next = this.used.nextClearBit(id + 1);

        return id;
    }

    public void release(int id) {
        this.used.clear(id);

        if (id < this.next) {
            this.next = id;
        }
    }

    public void clear() {
        this.used.clear();
        this.next = 0;
    }
}