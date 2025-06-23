package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

public abstract class ExpDecayLinear2DEstimator<C> extends Abstract2DLinearEstimator<
        C,
        ExpDecayLinear2DEstimator.ClearingLinearRegressionBatch<C>,
        ExpDecayLinear2DEstimator.ExpDecayLinearFunction<C>> {
    private final float newDataRatio;
    private final int initialSampleTarget;
    private final int minBatchSize;

    public ExpDecayLinear2DEstimator(float newDataRatio, int initialSampleTarget, int minBatchSize, long initialOutput) {
        super(initialOutput);
        this.newDataRatio = newDataRatio;
        this.initialSampleTarget = initialSampleTarget;
        this.minBatchSize = minBatchSize;
    }

    protected static class ClearingLinearRegressionBatch<C> extends Abstract2DLinearEstimator.LinearRegressionBatch<C> {
        boolean deferredClear = false;

        @Override
        public void reset() {
            if (!this.deferredClear) {
                this.clear();
            }
            this.deferredClear = false;
        }
        
        private boolean checkUpdateDefer(int minBatchSize) {
            if (this.size() < minBatchSize) {
                this.deferredClear = true;
                return true;
            }
            return false;
        }
    }

    @Override
    protected ClearingLinearRegressionBatch<C> createNewDataBatch() {
        return new ClearingLinearRegressionBatch<>();
    }

    protected static class ExpDecayLinearFunction<C> extends Abstract2DLinearEstimator.LinearFunction<
            C,
            ExpDecayLinear2DEstimator.ClearingLinearRegressionBatch<C>> {
        // the maximum fraction of the total weight that new data can have
        private final float newDataRatioInv;
        // how many samples we want to have at least before we start diminishing the new data's weight
        private final int initialSampleTarget;
        private final int minBatchSize;

        private float xMeanOld = 0;
        private float yMeanOld = 0;
        private float covarianceOld = 0;
        private float varianceOld = 0;

        public ExpDecayLinearFunction(float newDataRatio, int initialSampleTarget, int minBatchSize, long initialOutput) {
            super(initialOutput);
            this.newDataRatioInv = 1.0f / newDataRatio;
            this.initialSampleTarget = initialSampleTarget;
            this.minBatchSize = minBatchSize;
        }

        @Override
        public void update(ClearingLinearRegressionBatch<C> batch) {
            if (batch.isEmpty() || batch.checkUpdateDefer(this.minBatchSize)) {
                return;
            }

            // condition the weight to gather at least the initial sample target, and then weight the new data with a ratio
            var newDataSize = batch.size();
            var totalSamples = this.gatheredSamples + newDataSize;
            float oldDataWeight;
            float totalWeight;
            if (totalSamples <= this.initialSampleTarget) {
                totalWeight = totalSamples;
                oldDataWeight = this.gatheredSamples;
                this.gatheredSamples = totalSamples;
            } else {
                oldDataWeight = newDataSize * this.newDataRatioInv - newDataSize;
                totalWeight = oldDataWeight + newDataSize;
            }

            var totalWeightInv = 1.0f / totalWeight;

            // calculate the weighted mean along both axes
            long xSum = 0;
            long ySum = 0;
            for (var data : batch) {
                xSum += data.x();
                ySum += data.y();
            }
            var xMean = (this.xMeanOld * oldDataWeight + xSum) * totalWeightInv;
            var yMean = (this.yMeanOld * oldDataWeight + ySum) * totalWeightInv;

            // the covariance and variance are calculated from the differences to the mean
            var covarianceSum = 0.0f;
            var varianceSum = 0.0f;
            for (var data : batch) {
                var xDelta = data.x() - xMean;
                var yDelta = data.y() - yMean;
                covarianceSum += xDelta * yDelta;
                varianceSum += xDelta * xDelta;
            }

            if (varianceSum == 0) {
                return;
            }

            covarianceSum += this.covarianceOld * oldDataWeight;
            varianceSum += this.varianceOld * oldDataWeight;

            // negative slopes are clamped to produce a flat line if necessary
            this.slope = Math.max(0, covarianceSum / varianceSum);
            this.yIntercept = yMean - this.slope * xMean;

            this.xMeanOld = xMean;
            this.yMeanOld = yMean;
            this.covarianceOld = covarianceSum * totalWeightInv;
            this.varianceOld = varianceSum * totalWeightInv;
        }
    }

    @Override
    protected ExpDecayLinearFunction<C> createNewModel() {
        return new ExpDecayLinearFunction<>(this.newDataRatio, this.initialSampleTarget, this.minBatchSize, this.initialOutput);
    }
}
