package net.caffeinemc.mods.sodium.client.render.chunk.map;

import it.unimi.dsi.fastutil.longs.*;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.TimeUnit;

public class ChunkTracker implements ClientChunkEventListener {
    private static final int CHUNK_NOT_READY = -1;
    private static final int CHUNK_READY_WITH_ALL_NEIGHBORS = 0;
    private static final long EDGE_RENDER_SETTLE_DELAY_NANOS = TimeUnit.SECONDS.toNanos(2);

    private final Long2IntOpenHashMap chunkStatus = new Long2IntOpenHashMap();
    private final LongOpenHashSet chunkReady = new LongOpenHashSet();
    private final Long2IntOpenHashMap chunkReadyEdgeMasks = new Long2IntOpenHashMap();

    private final LongSet unloadQueue = new LongOpenHashSet();
    private final LongSet loadQueue = new LongOpenHashSet();

    private long edgeRenderAllowedAfter;
    private boolean edgeRenderSettling = true;

    public ChunkTracker() {
        this.chunkReadyEdgeMasks.defaultReturnValue(CHUNK_NOT_READY);
        this.deferEdgeRendering();
    }

    @Override
    public void updateMapCenter(int chunkX, int chunkZ) {
        this.deferEdgeRendering();
        this.refreshReadyChunks();
    }

    @Override
    public void updateLoadDistance(int loadDistance) {
        this.deferEdgeRendering();
        this.refreshReadyChunks();
    }

    @Override
    public void onChunkStatusAdded(int x, int z, int flags) {
        var key = ChunkPos.asLong(x, z);

        var prev = this.chunkStatus.get(key);
        var cur = prev | flags;

        if (prev == cur) {
            return;
        }

        this.chunkStatus.put(key, cur);
        this.updateAfterChunkStatusChanged(x, z);
    }

    @Override
    public void onChunkStatusRemoved(int x, int z, int flags) {
        var key = ChunkPos.asLong(x, z);

        var prev = this.chunkStatus.get(key);
        int cur = prev & ~flags;

        if (prev == cur) {
            return;
        }

        if (cur == this.chunkStatus.defaultReturnValue()) {
            this.chunkStatus.remove(key);
        } else {
            this.chunkStatus.put(key, cur);
        }

        this.updateAfterChunkStatusChanged(x, z);
    }

    private void updateAfterChunkStatusChanged(int x, int z) {
        if (this.deferEdgeRendering()) {
            this.refreshReadyChunks();
        } else {
            this.updateNeighbors(x, z);
        }
    }

    private void updateNeighbors(int x, int z) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    private void updateMerged(int x, int z) {
        long key = ChunkPos.asLong(x, z);

        this.setChunkReady(key, this.getChunkReadyEdgeMask(x, z));
    }

    private int getChunkReadyEdgeMask(int x, int z) {
        int flags = this.chunkStatus.get(ChunkPos.asLong(x, z));

        if (!this.hasAllData(flags)) {
            return CHUNK_NOT_READY;
        }

        if (this.canRenderLoadedChunkEdges()) {
            int missingNeighborMask = 0;
            int neighborIndex = 0;

            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    if (ox == 0 && oz == 0) {
                        continue;
                    }

                    int neighborFlags = this.chunkStatus.get(ChunkPos.asLong(ox + x, oz + z));

                    if (neighborFlags == this.chunkStatus.defaultReturnValue()) {
                        missingNeighborMask |= 1 << neighborIndex;
                    } else if (!this.hasAllData(neighborFlags)) {
                        return CHUNK_NOT_READY;
                    }

                    neighborIndex++;
                }
            }

            if (missingNeighborMask != 0) {
                return missingNeighborMask;
            }
        }

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                flags &= this.chunkStatus.get(ChunkPos.asLong(ox + x, oz + z));
            }
        }

        return flags == ChunkStatus.FLAG_ALL ? CHUNK_READY_WITH_ALL_NEIGHBORS : CHUNK_NOT_READY;
    }

    private boolean hasAllData(int flags) {
        return (flags & ChunkStatus.FLAG_ALL) == ChunkStatus.FLAG_ALL;
    }

    private boolean canRenderLoadedChunkEdges() {
        return SodiumClientMod.options().performance.renderLoadedChunkEdges && !this.edgeRenderSettling;
    }

    private void setChunkReady(long key, int edgeMask) {
        if (edgeMask != CHUNK_NOT_READY) {
            if (this.chunkReady.add(key)) {
                this.chunkReadyEdgeMasks.put(key, edgeMask);

                if (!this.unloadQueue.remove(key)) {
                    this.loadQueue.add(key);
                }
            } else {
                int previousEdgeMask = this.chunkReadyEdgeMasks.put(key, edgeMask);

                if (previousEdgeMask != edgeMask && !this.loadQueue.contains(key)) {
                    this.unloadQueue.add(key);
                    this.loadQueue.add(key);
                }
            }
        } else {
            this.chunkReadyEdgeMasks.remove(key);

            if (this.chunkReady.remove(key) && !this.loadQueue.remove(key)) {
                this.unloadQueue.add(key);
            }
        }
    }

    private void setChunkReadyDuringRefresh(long key, int edgeMask, Long2IntOpenHashMap previousReadyEdgeMasks) {
        if (edgeMask == CHUNK_NOT_READY) {
            return;
        }

        this.chunkReady.add(key);
        this.chunkReadyEdgeMasks.put(key, edgeMask);

        if (!previousReadyEdgeMasks.containsKey(key)) {
            this.loadQueue.add(key);
        } else if (previousReadyEdgeMasks.get(key) != edgeMask) {
            this.unloadQueue.add(key);
            this.loadQueue.add(key);
        }
    }

    private void unloadChunksRemovedDuringRefresh(Long2IntOpenHashMap previousReadyEdgeMasks) {
        var iterator = previousReadyEdgeMasks.keySet().iterator();

        while (iterator.hasNext()) {
            var key = iterator.nextLong();

            if (!this.chunkReady.contains(key)) {
                this.unloadQueue.add(key);
            }
        }
    }

    private boolean deferEdgeRendering() {
        boolean wasRenderingLoadedChunkEdges = this.canRenderLoadedChunkEdges();

        this.edgeRenderAllowedAfter = System.nanoTime() + EDGE_RENDER_SETTLE_DELAY_NANOS;
        this.edgeRenderSettling = true;

        return wasRenderingLoadedChunkEdges;
    }

    private void refreshIfEdgeRenderingSettled() {
        if (!this.edgeRenderSettling || System.nanoTime() < this.edgeRenderAllowedAfter) {
            return;
        }

        this.edgeRenderSettling = false;
        this.refreshReadyChunks();
    }

    public void refreshReadyChunks() {
        var previousReadyEdgeMasks = new Long2IntOpenHashMap(this.chunkReadyEdgeMasks);
        previousReadyEdgeMasks.defaultReturnValue(CHUNK_NOT_READY);

        this.chunkReady.clear();
        this.chunkReadyEdgeMasks.clear();
        this.unloadQueue.clear();
        this.loadQueue.clear();

        var iterator = this.chunkStatus.keySet().iterator();

        while (iterator.hasNext()) {
            var key = iterator.nextLong();

            this.setChunkReadyDuringRefresh(
                    key,
                    this.getChunkReadyEdgeMask(ChunkPos.getX(key), ChunkPos.getZ(key)),
                    previousReadyEdgeMasks);
        }

        this.unloadChunksRemovedDuringRefresh(previousReadyEdgeMasks);
    }

    public LongCollection getReadyChunks() {
        return LongSets.unmodifiable(this.chunkReady);
    }

    public void forEachEvent(ChunkEventHandler loadEventHandler, ChunkEventHandler unloadEventHandler) {
        this.refreshIfEdgeRenderingSettled();

        forEachChunk(this.unloadQueue, unloadEventHandler);
        this.unloadQueue.clear();

        forEachChunk(this.loadQueue, loadEventHandler);
        this.loadQueue.clear();
    }

    public static void forEachChunk(LongCollection queue, ChunkEventHandler handler) {
        var iterator = queue.iterator();

        while (iterator.hasNext()) {
            var pos = iterator.nextLong();

            var x = ChunkPos.getX(pos);
            var z = ChunkPos.getZ(pos);

            handler.apply(x, z);
        }
    }

    public interface ChunkEventHandler {
        void apply(int x, int z);
    }
}
