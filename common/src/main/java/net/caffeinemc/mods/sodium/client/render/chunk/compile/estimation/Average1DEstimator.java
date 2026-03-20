package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import net.caffeinemc.mods.sodium.client.util.MathUtil;

import java.util.Locale;

public abstract class Average1DEstimator<C> extends Estimator<C, Average1DEstimator.Value<C>, Average1DEstimator.ValueBatch<C>, Void, Long, Average1DEstimator.Average<C>> {
    private final double newDataRatio;
    private final long initialEstimate;

    public Average1DEstimator(double newDataRatio, long initialEstimate) {
        this.newDataRatio = newDataRatio;
        this.initialEstimate = initialEstimate;
    }

    public interface Value<PointCategory> extends DataPoint<PointCategory> {
        long value();
    }

    protected static class ValueBatch<BatchCategory> implements Estimator.DataBatch<Value<BatchCategory>> {
        private long valueSum;
        private long count;

        @Override
        public void addDataPoint(Value<BatchCategory> input) {
            this.valueSum += input.value();
            this.count++;
        }

        @Override
        public void reset() {
            this.valueSum = 0;
            this.count = 0;
        }

        public double getAverage() {
            return ((double) this.valueSum) / this.count;
        }
    }

    @Override
    protected ValueBatch<C> createNewDataBatch() {
        return new ValueBatch<>();
    }

    protected static class Average<C> implements Estimator.Model<Void, Long, ValueBatch<C>> {
        private final double newDataRatio;
        private boolean hasRealData = false;
        private double average;

        public Average(double newDataRatio, double initialValue) {
            this.average = initialValue;
            this.newDataRatio = newDataRatio;
        }

        @Override
        public void update(ValueBatch<C> batch) {
            if (batch.count > 0) {
                if (this.hasRealData) {
                    this.average = MathUtil.exponentialMovingAverage(this.average, batch.getAverage(), this.newDataRatio);
                } else {
                    this.average = batch.getAverage();
                    this.hasRealData = true;
                }
            }
        }

        @Override
        public Long predict(Void input) {
            return (long) this.average;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%.0f", this.average);
        }
    }

    @Override
    protected Average<C> createNewModel() {
        return new Average<>(this.newDataRatio, this.initialEstimate);
    }

    public Long predict(C category) {
        return super.predict(category, null);
    }
}
