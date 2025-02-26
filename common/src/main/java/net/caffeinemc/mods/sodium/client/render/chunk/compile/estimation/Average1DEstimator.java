package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import net.caffeinemc.mods.sodium.client.util.MathUtil;

import java.util.Locale;

public abstract class Average1DEstimator<Category> extends Estimator<Category, Average1DEstimator.Value<Category>, Average1DEstimator.ValueBatch<Category>, Void, Long, Average1DEstimator.Average<Category>> {
    private final float newDataRatio;
    private final long initialEstimate;

    public Average1DEstimator(float newDataRatio, long initialEstimate) {
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

        public float getAverage() {
            return ((float) this.valueSum) / this.count;
        }
    }

    @Override
    protected ValueBatch<Category> createNewDataBatch() {
        return new ValueBatch<>();
    }

    protected static class Average<ModelCategory> implements Estimator.Model<Void, Long, ValueBatch<ModelCategory>, Average<ModelCategory>> {
        private final float newDataRatio;
        private boolean hasRealData = false;
        private float average;

        public Average(float newDataRatio, float initialValue) {
            this.average = initialValue;
            this.newDataRatio = newDataRatio;
        }

        @Override
        public Average<ModelCategory> update(ValueBatch<ModelCategory> batch) {
            if (batch.count > 0) {
                if (this.hasRealData) {
                    this.average = MathUtil.exponentialMovingAverage(this.average, batch.getAverage(), this.newDataRatio);
                } else {
                    this.average = batch.getAverage();
                    this.hasRealData = true;
                }
            }

            return this;
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
    protected Average<Category> createNewModel() {
        return new Average<>(this.newDataRatio, this.initialEstimate);
    }

    public Long predict(Category category) {
        return super.predict(category, null);
    }
}
