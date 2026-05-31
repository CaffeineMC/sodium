package net.caffeinemc.mods.sodium.client.util;

import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongHeaps;
import it.unimi.dsi.fastutil.objects.Reference2LongArrayMap;

import java.util.Arrays;
import java.util.Comparator;

// Maintains a ring buffer of frame durations populated from DebugScreenOverlay#logFrameDuration. We keep our own buffer because vanilla's FpsAccumulator only retains 240 samples, which gives only a fractional sample in the p99.9 tail.
public final class FrameTimeStatistics {
    private static final int SAMPLE_COUNT = 1000;

    public record Percentile(String name, int window, float p) {
    }

    private static final Percentile[] PERCENTILES = new Percentile[] {
            new Percentile("p50", 200, 0.5f),
            new Percentile("p98", Integer.MAX_VALUE, 0.98f),
            new Percentile("p99.5",Integer.MAX_VALUE, 0.995f)
    };

    // PERCENTILES sorted (window asc, p desc) so each step of compute() either keeps the heap window or extends it with older sample, and within an equal-window run popDownTo only shrinks the heap.
    private static final Percentile[] CALCULATION_ORDER = computeCalculationOrder();

    public static final FrameTimeStatistics INSTANCE = new FrameTimeStatistics();

    private static final LongComparator HEAP_COMPARATOR = LongComparators.OPPOSITE_COMPARATOR;

    private final long[] samples = new long[SAMPLE_COUNT];
    private int writeIndex = 0;
    private int sampleSize = 0;

    private long[] heap;
    private volatile Reference2LongArrayMap<Percentile> cached;

    private FrameTimeStatistics() {
    }

    public Reference2LongArrayMap<Percentile> get() {
        if (this.cached == null) {
            this.cached = this.compute();
        }
        return this.cached;
    }

    // called from the render thread for every frame whose duration vanilla logs
    public void logSample(long frameDuration) {
        this.samples[this.writeIndex] = frameDuration;
        this.writeIndex = (this.writeIndex + 1) % SAMPLE_COUNT;
        if (this.sampleSize < SAMPLE_COUNT) {
            this.sampleSize++;
        }
    }

    public void invalidate() {
        this.cached = null;
    }

    private Reference2LongArrayMap<Percentile> compute() {
        int n = this.sampleSize;
        if (n <= 0) {
            return null;
        }

        if (this.heap == null) {
            this.heap = new long[SAMPLE_COUNT];
        }
        var heap = this.heap;

        // pre-populate so iteration order in the result map matches the display order in PERCENTILES
        var results = new Reference2LongArrayMap<Percentile>(PERCENTILES.length);
        for (Percentile percentile : PERCENTILES) {
            results.put(percentile, -1L);
        }

        // currentWindow is how many samples we've copied from the ring buffer into our calculation array so far
        int currentWindow = 0;
        int heapSize = 0;
        for (var percentile : CALCULATION_ORDER) {
            int window = Math.min(percentile.window(), n);

            if (window > currentWindow) {
                // append the next-older block of samples and re-heapify the full window; popped entries from the previous group rejoin the heap automatically
                this.copyMostRecentSamples(heap, currentWindow, window - currentWindow);
                LongHeaps.makeHeap(heap, window, HEAP_COMPARATOR);
                currentWindow = window;
                heapSize = window;
            }

            heapSize = popDownTo(heap, heapSize, window - rankFromTop(percentile.p(), window));
            results.put(percentile, heap[0]);
        }
        return results;
    }

    // copy ring-buffer samples into dest[destOffset..destOffset + count), where destOffset is the index of the first sample expressed as "k-th most recent" (0 = newest). One arraycopy if the source range doesn't wrap, two if it does. The order within dest is irrelevant because the consumer rebuilds the heap.
    private void copyMostRecentSamples(long[] dest, int destOffset, int count) {
        if (count <= 0) {
            return;
        }
        int srcStart = Math.floorMod(this.writeIndex - destOffset - count, SAMPLE_COUNT);
        int firstChunk = Math.min(count, SAMPLE_COUNT - srcStart);
        System.arraycopy(this.samples, srcStart, dest, destOffset, firstChunk);
        if (firstChunk < count) {
            System.arraycopy(this.samples, 0, dest, destOffset + firstChunk, count - firstChunk);
        }
    }

    private static Percentile[] computeCalculationOrder() {
        var order = Arrays.copyOf(PERCENTILES, PERCENTILES.length);

        // sort by window ascending, p descending
        var byWindowThenPDesc = Comparator
                .comparingInt(Percentile::window)
                .thenComparing(Percentile::p, Comparator.reverseOrder());

        Arrays.sort(order, byWindowThenPDesc);

        return order;
    }

    // index of the p-quantile in a descending sequence of length n
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
