package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

public class LimitedResourceBudget implements UploadResourceBudget {
    private long duration;
    private long size;

    public LimitedResourceBudget(long duration, long size) {
        this.duration = duration;
        this.size = size;
    }

    @Override
    public boolean isAvailable() {
        return this.duration > 0 && this.size > 0;
    }

    @Override
    public void consume(long duration, long size) {
        this.duration -= duration;
        this.size -= size;
    }
}
