package net.caffeinemc.mods.sodium.client.render.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.minecraft.core.SectionPos;

public class QueuedSectionStorage implements SectionStorage {
    private final Long2ReferenceMap<RenderSection> sections = new Long2ReferenceOpenHashMap<>();
    private final Long2ReferenceMap<RenderSection> queuedPuts = new Long2ReferenceOpenHashMap<>();
    private final LongSet queuedRemovals = new LongOpenHashSet();
    private boolean isQueueing = false;
    private int lastPutQueueSize = 0;
    private int lastRemoveQueueSize = 0;

    @Override
    public RenderSection getCurrent(SectionPos pos) {
        return this.sections.get(pos.asLong());
    }

    @Override
    public RenderSection getCurrent(int x, int y, int z) {
        return this.sections.get(SectionPos.asLong(x, y, z));
    }

    @Override
    public RenderSection getCurrent(long key) {
        return this.sections.get(key);
    }

    @Override
    public RenderSection getConsistent(long key) {
        if (!this.isQueueing) {
            return this.sections.get(key);
        }

        var queuedPut = this.queuedPuts.get(key);
        if (queuedPut != null) {
            return queuedPut;
        }

        if (this.queuedRemovals.contains(key)) {
            return null;
        }

        return this.sections.get(key);
    }

    @Override
    public boolean hasSectionConsistent(long key) {
        return this.getConsistent(key) != null;
    }

    @Override
    public void queuePut(long key, RenderSection newSection) {
        if (this.isQueueing) {
            this.queuedRemovals.remove(key);
            this.queuedPuts.put(key, newSection);
        } else {
            this.sections.put(key, newSection);
        }
    }

    @Override
    public RenderSection queueRemove(long key) {
        if (this.isQueueing) {
            var present = this.sections.get(key);
            if (present != null) {
                this.queuedRemovals.add(key);
            }
            var removedPut = this.queuedPuts.remove(key);
            if (removedPut != null) {
                return removedPut;
            }
            return present;
        } else {
            return this.sections.remove(key);
        }
    }

    @Override
    public void deleteAll() {
        for (var section : this.sections.values()) {
            section.delete();
        }
        for (var section : this.queuedPuts.values()) {
            section.delete();
        }
        this.sections.clear();
        this.queuedPuts.clear();
        this.queuedRemovals.clear();
    }

    @Override
    public int size() {
        return this.sections.size();
    }

    @Override
    public void startSafeReadPhase() {
        this.isQueueing = true;
    }

    @Override
    public void endSafeReadPhase() {
        if (this.isQueueing && (!this.queuedPuts.isEmpty() || !this.queuedRemovals.isEmpty())) {
            this.lastPutQueueSize = this.queuedPuts.size();
            this.lastRemoveQueueSize = this.queuedRemovals.size();

            // interleave puts and removals to avoid hashmap rehashing if possible
            var putIterator = this.queuedPuts.long2ReferenceEntrySet().iterator();
            var removeIterator = this.queuedRemovals.longIterator();
            var madeChange = true;
            while (madeChange) {
                madeChange = false;
                if (putIterator.hasNext()) {
                    var entry = putIterator.next();
                    this.sections.put(entry.getLongKey(), entry.getValue());
                    madeChange = true;
                }
                if (removeIterator.hasNext()) {
                    this.sections.remove(removeIterator.nextLong());
                    madeChange = true;
                }
            }

            this.queuedPuts.clear();
            this.queuedRemovals.clear();
        }

        this.isQueueing = false;
    }

    @Override
    public String getDebugInfo() {
        return String.format("QD: put %03d/del %03d", this.lastPutQueueSize, this.lastRemoveQueueSize);
    }
}
