package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

public record UploadDuration(long uploadDuration, long size) implements ExpDecayLinear2DEstimator.DataPair<Void> {
    @Override
    public long x() {
        return this.size;
    }

    @Override
    public long y() {
        return this.uploadDuration;
    }

    @Override
    public Void category() {
        return null;
    }
}
