package me.jellysquid.mods.sodium.client.render.particle.cache;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.caffeinemc.mods.sodium.api.util.RawUVs;

import java.util.*;

public class ParticleTextureCache {
    private static final int DEFAULT_CAPACITY = 64;

    private RawUVs[] rawUVs;
    private final Object2IntOpenHashMap<RawUVs> uvToIndex = new Object2IntOpenHashMap<>();
    private final TextureUsageMonitor usageMonitor = new TextureUsageMonitor();

    private final IntArrayFIFOQueue freeIndices = new IntArrayFIFOQueue();
    private int topIndex = 0;

    public ParticleTextureCache() {
        this.rawUVs = new RawUVs[DEFAULT_CAPACITY];
    }

    public int getTopIndex() {
        return topIndex;
    }

    public int getUvIndex(RawUVs uvs) {
        int use = uvToIndex.computeIfAbsent(uvs, key -> {
            RawUVs uvsKey = (RawUVs) key;
            int index = freeIndices.isEmpty() ? topIndex++ : freeIndices.dequeueInt();

            ensureCapacity(index);
            rawUVs[index] = uvsKey;
            return index;
        });

        usageMonitor.markUsed(use);
        return use;
    }

    /**
     * @return The array of RawUVs that should be uploaded
     */
    public RawUVs[] update() {
        IntList toRemove = this.usageMonitor.update();
        for (int i = 0; i < toRemove.size(); i++) {
            int index = toRemove.getInt(i);

            RawUVs uvs = rawUVs[index];
            rawUVs[index] = null;

            this.freeIndices.enqueue(index);
            uvToIndex.removeInt(uvs);
        }
        return this.rawUVs;
    }

    private void ensureCapacity(int high) {
        if (high >= this.rawUVs.length) {
            reallocRawUVs(high);
        }
    }

    private void reallocRawUVs(int high) {
        int newCapacity = this.rawUVs.length;
        while (high >= newCapacity) {
            newCapacity += (newCapacity >> 1);
        }
        RawUVs[] newArray = new RawUVs[newCapacity];
        System.arraycopy(rawUVs, 0, newArray, 0, rawUVs.length);
        this.rawUVs = newArray;
    }
}
