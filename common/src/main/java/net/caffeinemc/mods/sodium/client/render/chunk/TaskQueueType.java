package net.caffeinemc.mods.sodium.client.render.chunk;

public enum TaskQueueType {
    ZERO_FRAME_DEFER,
    ONE_FRAME_DEFER,
    ALWAYS_DEFER,
    INITIAL_BUILD;

    public boolean allowsUnlimitedUploadSize() {
        return this == ZERO_FRAME_DEFER;
    }

    public int queueSizeLimit() {
        return this == INITIAL_BUILD ? 128 : Integer.MAX_VALUE;
    }
}
