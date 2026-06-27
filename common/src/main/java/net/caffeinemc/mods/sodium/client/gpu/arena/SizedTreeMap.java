package net.caffeinemc.mods.sodium.client.gpu.arena;

import java.util.Map;
import java.util.TreeMap;

/**
 * Stores a set of sized objects that typically don't collide but may.
 *
 * @param <V> the type of sized object stored in the map
 */
public class SizedTreeMap<V extends SizedTreeMap.Sized> extends TreeMap<Long, V> {
    private Map.Entry<Long, V> cachedHighestEntry = null;

    public SizedTreeMap() {
        super(Long::compareUnsigned);
    }

    interface Sized {
        long getSize();
        long getIdentifier();

        default long makeKey() {
            return (getSize() << 32) | (getIdentifier() & 0xFFFFFFFFL);
        }
    }

    public V addSized(V value) {
        var key = value.makeKey();
        var previous = super.put(key, value);
        if (this.cachedHighestEntry != null && Long.compareUnsigned(key, this.cachedHighestEntry.getKey()) > 0) {
            this.cachedHighestEntry = null;
        }
        return previous;
    }

    public V removeSized(V value) {
        var removed = super.remove(value.makeKey());
        clearCacheWithRemoved(removed);
        return removed;
    }

    private void clearCacheWithRemoved(V removed) {
        if (this.cachedHighestEntry != null && this.cachedHighestEntry.getValue() == removed) {
            this.cachedHighestEntry = null;
        }
    }

    public V removeFirstOfSizeAtLeast(long requiredSize) {
        var tailMap = this.tailMap(requiredSize << 32);
        if (tailMap.isEmpty()) {
            return null;
        }
        var removed = tailMap.pollFirstEntry().getValue();
        clearCacheWithRemoved(removed);
        return removed;
    }

    public V removeNext() {
        var entry = this.pollFirstEntry();
        if (entry == null) {
            return null;
        }
        var removed = entry.getValue();
        clearCacheWithRemoved(removed);
        return removed;
    }

    public Map.Entry<Long, V> getLargestEntry() {
        if (this.cachedHighestEntry == null && !this.isEmpty()) {
            this.cachedHighestEntry = this.lastEntry();
        }
        return this.cachedHighestEntry;
    }

    public long getLargestSize() {
        var entry = this.getLargestEntry();
        return entry != null ? entry.getKey() >>> 32 : 0;
    }
}
