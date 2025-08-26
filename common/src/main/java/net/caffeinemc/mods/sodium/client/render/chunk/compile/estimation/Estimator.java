package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import java.util.Map;

/**
 * This generic model learning class that can be used to estimate values based on a set of data points. It performs batch-wise model updates. The actual data aggregation and model updates are delegated to the implementing classes. The estimator stores multiple models in a map, one for each category.
 *
 * @param <TCategory> The type of the category key
 * @param <TPoint> A data point contains a category and one piece of data
 * @param <TBatch> A data batch contains multiple data points
 * @param <TInput> The input to the model
 * @param <TOutput> The output of the model
 * @param <TModel> The model that is used to predict values
 */
public abstract class Estimator<
        TCategory,
        TPoint extends Estimator.DataPoint<TCategory>,
        TBatch extends Estimator.DataBatch<TPoint>,
        TInput,
        TOutput,
        TModel extends Estimator.Model<TInput, TOutput, TBatch, TModel>> {
    protected final Map<TCategory, TModel> models = createMap();
    protected final Map<TCategory, TBatch> batches = createMap();

    protected interface DataBatch<TBatchPoint> {
        void addDataPoint(TBatchPoint input);

        void reset();
    }

    protected interface DataPoint<TPointCategory> {
        TPointCategory category();
    }

    protected interface Model<TModelInput, TModelOutput, TModelBatch, TModelSelf extends Model<TModelInput, TModelOutput, TModelBatch, TModelSelf>> {
        TModelSelf update(TModelBatch batch);

        TModelOutput predict(TModelInput input);
    }

    protected abstract TBatch createNewDataBatch();

    protected abstract TModel createNewModel();

    protected abstract <T> Map<TCategory, T> createMap();

    public void addData(TPoint data) {
        var category = data.category();
        var batch = this.batches.get(category);
        if (batch == null) {
            batch = this.createNewDataBatch();
            this.batches.put(category, batch);
        }
        batch.addDataPoint(data);
    }

    private TModel ensureModel(TCategory category) {
        var model = this.models.get(category);
        if (model == null) {
            model = this.createNewModel();
            this.models.put(category, model);
        }
        return model;
    }

    public void updateModels() {
        this.batches.forEach((category, aggregator) -> {
            var oldModel = this.ensureModel(category);

            // update the model and store it back if it returned a new model
            var newModel = oldModel.update(aggregator);
            if (newModel != oldModel) {
                this.models.put(category, newModel);
            }

            aggregator.reset();
        });
    }

    public TOutput predict(TCategory category, TInput input) {
        return this.ensureModel(category).predict(input);
    }

    public String toString(TCategory category) {
        var model = this.models.get(category);
        if (model == null) {
            return "-";
        }
        return model.toString();
    }
}
