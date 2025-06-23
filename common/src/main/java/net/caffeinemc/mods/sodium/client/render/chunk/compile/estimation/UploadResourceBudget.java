package net.caffeinemc.mods.sodium.client.render.chunk.compile.estimation;

public interface UploadResourceBudget {
    boolean isAvailable();

    void consume(long duration, long size);
}
