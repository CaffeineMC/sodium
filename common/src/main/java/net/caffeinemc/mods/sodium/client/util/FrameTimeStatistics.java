package net.caffeinemc.mods.sodium.client.util;

import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongHeaps;
import net.minecraft.util.debugchart.SampleStorage;

public final class FrameTimeStatistics {
    public record Result(int count, long avg, long p50, long p99, long p999) {
    }

    public static final FrameTimeStatistics INSTANCE = new FrameTimeStatistics();

    private static final Result EMPTY = new Result(0, 0L, 0L, 0L, 0L);
    private static final LongComparator HEAP_COMPARATOR = LongComparators.OPPOSITE_COMPARATOR;

    private long[] frameTimesCopy;
    private volatile Result cached = EMPTY;

    private FrameTimeStatistics() {
    }

    public Result get() {
        return this.cached;
    }

    public void update(SampleStorage storage) {
        this.cached = this.compute(storage);
    }

    private Result compute(SampleStorage storage) {
        int sampleCount = storage.size();
        if (sampleCount <= 0) {
            return EMPTY;
        }

        if (this.frameTimesCopy == null || this.frameTimesCopy.length < sampleCount) {
            this.frameTimesCopy = new long[storage.capacity()];
        }
        var heap = this.frameTimesCopy;

        // make a copy so that we can mess with the samples
        long sum = 0L;
        for (int i = 0; i < sampleCount; i++) {
            long sample = storage.get(i);
            heap[i] = sample;
            sum += sample;
        }

        // make a max heap (descending frame time)
        LongHeaps.makeHeap(heap, sampleCount, HEAP_COMPARATOR);

        // visit such that we incrementally remove entries from the heap of samples
        int heapSize = popDownTo(heap, sampleCount, sampleCount - rankFromTop(0.999, sampleCount));
        long p999 = heap[0];
        heapSize = popDownTo(heap, heapSize, sampleCount - rankFromTop(0.99, sampleCount));
        long p99 = heap[0];
        popDownTo(heap, heapSize, sampleCount - rankFromTop(0.5, sampleCount));
        long p50 = heap[0];

        // compute average with rounding to nearest
        long average = (sum + sampleCount / 2L) / sampleCount;
        return new Result(sampleCount, average, p50, p99, p999);
    }

    // index of the p-quantile a descending sequence of length n
    private static int rankFromTop(double p, int n) {
        int rank = (int) Math.floor((1.0 - p) * n);
        if (rank < 0) {
            return 0;
        }
        if (rank >= n) {
            return n - 1;
        }
        return rank;
    }

    // pop maxes (swap with tail, sift down) until the heap shrinks to targetSize
    private static int popDownTo(long[] heap, int heapSize, int targetSize) {
        while (heapSize > targetSize) {
            heapSize--;
            heap[0] = heap[heapSize];
            if (heapSize > 0) {
                LongHeaps.downHeap(heap, heapSize, 0, HEAP_COMPARATOR);
            }
        }
        return heapSize;
    }
}
