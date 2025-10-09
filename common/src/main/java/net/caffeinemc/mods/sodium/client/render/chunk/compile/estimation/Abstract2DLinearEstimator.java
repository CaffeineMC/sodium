package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Locale;

public abstract class Abstract2DLinearEstimator<
        C,
        TBatch extends Estimator.DataBatch<Abstract2DLinearEstimator.DataPair<C>>,
        TModel extends Abstract2DLinearEstimator.LinearFunction<C, TBatch>
        > extends Estimator<
        C,
        Abstract2DLinearEstimator.DataPair<C>,
        TBatch,
        Long,
        Long,
        TModel> {
    protected final long initialOutput;

    public Abstract2DLinearEstimator(long initialOutput) {
        this.initialOutput = initialOutput;
    }

    public interface DataPair<C> extends DataPoint<C> {
        long x();

        long y();
    }

    protected abstract static class LinearRegressionBatch<C> extends ObjectArrayList<DataPair<C>> implements DataBatch<DataPair<C>> {
        @Override
        public void addDataPoint(DataPair<C> input) {
            this.add(input);
        }
    }

    protected abstract static class LinearFunction<C, TBatch extends DataBatch<DataPair<C>>> implements Model<Long, Long, TBatch> {
        protected final long initialOutput;
        protected double yIntercept;
        protected double slope;
        protected int gatheredSamples = 0;

        public LinearFunction(long initialOutput) {
            this.initialOutput = initialOutput;
        }

        @Override
        public Long predict(Long input) {
            if (this.gatheredSamples == 0) {
                return this.initialOutput;
            }

            return (long) (this.yIntercept + this.slope * input);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "s=%.2f,y=%.0f", this.slope, this.yIntercept);
        }
    }
}
