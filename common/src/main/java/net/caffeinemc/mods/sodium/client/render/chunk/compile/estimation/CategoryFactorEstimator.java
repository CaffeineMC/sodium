package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.util.MathUtil;

public class CategoryFactorEstimator<C> {
    private final Reference2FloatMap<C> aPerB = new Reference2FloatOpenHashMap<>();
    private final Reference2ReferenceMap<C, BatchDataAggregation> newData = new Reference2ReferenceOpenHashMap<>();
    private final float newDataFactor;
    private final long initialAEstimate;

    public CategoryFactorEstimator(float newDataFactor, long initialAEstimate) {
        this.newDataFactor = newDataFactor;
        this.initialAEstimate = initialAEstimate;
    }

    private static class BatchDataAggregation {
        private long aSum;
        private long bSum;

        public void addDataPoint(long a, long b) {
            this.aSum += a;
            this.bSum += b;
        }

        public void reset() {
            this.aSum = 0;
            this.bSum = 0;
        }

        public float getAPerBFactor() {
            return ((float) this.aSum) / this.bSum;
        }
    }

    public interface BatchEntry<C> {
        C getCategory();

        long getA();

        long getB();
    }

    public void addBatchEntry(BatchEntry<C> batchEntry) {
        var a = batchEntry.getA();
        var b = batchEntry.getB();

        // skip if b is 0 to prevent Infinity and NaN
        if (b == 0) {
            return;
        }

        var category = batchEntry.getCategory();
        var aggregation = this.newData.get(category);
        if (aggregation == null) {
            aggregation = new BatchDataAggregation();
            this.newData.put(category, aggregation);
        }
        aggregation.addDataPoint(a, b);
    }

    public void flushNewData() {
        this.newData.forEach((category, frameData) -> {
            var newFactor = frameData.getAPerBFactor();
            // if there was no data it results in NaN
            if (Float.isNaN(newFactor)) {
                return;
            }
            if (this.aPerB.containsKey(category)) {
                var oldFactor = this.aPerB.getFloat(category);
                var newValue = MathUtil.exponentialMovingAverage(oldFactor, newFactor, this.newDataFactor);
                this.aPerB.put(category, newValue);
            } else {
                this.aPerB.put(category, newFactor);
            }
            frameData.reset();
        });
    }

    public long estimateAWithB(C category, long b) {
        if (this.aPerB.containsKey(category)) {
            return (long) (this.aPerB.getFloat(category) * b);
        } else {
            return this.initialAEstimate;
        }
    }
}