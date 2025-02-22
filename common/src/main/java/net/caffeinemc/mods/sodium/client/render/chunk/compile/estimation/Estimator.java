package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

import java.util.Map;

/**
 * This generic model learning class that can be used to estimate values based on a set of data points. It performs batch-wise model updates. The actual data aggregation and model updates are delegated to the implementing classes. The estimator stores multiple models in a map, one for each category.
 *
 * @param <Category> The type of the category key
 * @param <Point> A data point contains a category and one piece of data
 * @param <Batch> A data batch contains multiple data points
 * @param <Input> The input to the model
 * @param <Output> The output of the model
 * @param <Model> The model that is used to predict values
 */
public abstract class Estimator<
        Category,
        Point extends Estimator.DataPoint<Category>,
        Batch extends Estimator.DataBatch<Point>,
        Input,
        Output,
        Model extends Estimator.Model<Input, Output, Batch, Model>> {
    protected final Map<Category, Model> models = createMap();
    protected final Map<Category, Batch> batches = createMap();

    protected interface DataBatch<BatchPoint> {
        void addDataPoint(BatchPoint input);

        void reset();
    }

    protected interface DataPoint<PointCategory> {
        PointCategory category();
    }

    protected interface Model<ModelInput, ModelOutput, ModelBatch, ModelSelf extends Model<ModelInput, ModelOutput, ModelBatch, ModelSelf>> {
        ModelSelf update(ModelBatch batch);

        ModelOutput predict(ModelInput input);
    }

    protected abstract Batch createNewDataBatch();

    protected abstract Model createNewModel();

    protected abstract <T> Map<Category, T> createMap();

    public void addData(Point data) {
        var category = data.category();
        var batch = this.batches.get(category);
        if (batch == null) {
            batch = this.createNewDataBatch();
            this.batches.put(category, batch);
        }
        batch.addDataPoint(data);
    }

    private Model ensureModel(Category category) {
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

    public Output predict(Category category, Input input) {
        return (Output) this.ensureModel(category).predict(input);
    }

    public String toString(Category category) {
        var model = this.models.get(category);
        if (model == null) {
            return "-";
        }
        return model.toString();
    }
}
