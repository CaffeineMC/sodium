package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;

public enum ChunkUpdateType {
    SORT(ChunkUpdateType.TYPE_SORT, Integer.MAX_VALUE, ChunkBuilder.LOW_EFFORT),
    REBUILD(ChunkUpdateType.TYPE_REBUILD, Integer.MAX_VALUE, ChunkBuilder.HIGH_EFFORT),
    REBUILD_WITH_SORT(ChunkUpdateType.TYPE_SORT | ChunkUpdateType.TYPE_REBUILD, Integer.MAX_VALUE, ChunkBuilder.HIGH_EFFORT),
    IMPORTANT_SORT(ChunkUpdateType.TYPE_SORT | ChunkUpdateType.TYPE_IMPORTANT, Integer.MAX_VALUE, ChunkBuilder.LOW_EFFORT),
    IMPORTANT_REBUILD(ChunkUpdateType.TYPE_REBUILD | ChunkUpdateType.TYPE_IMPORTANT, Integer.MAX_VALUE, ChunkBuilder.HIGH_EFFORT),
    IMPORTANT_REBUILD_WITH_SORT(ChunkUpdateType.TYPE_IMPORTANT | ChunkUpdateType.TYPE_SORT | ChunkUpdateType.TYPE_REBUILD, Integer.MAX_VALUE, ChunkBuilder.HIGH_EFFORT),
    INITIAL_BUILD(ChunkUpdateType.TYPE_INITIAL_REBUILD, 128, ChunkBuilder.HIGH_EFFORT);

    private static final int TYPE_SORT = 0b001;
    private static final int TYPE_REBUILD = 0b010;
    private static final int TYPE_IMPORTANT = 0b100;
    private static final int TYPE_INITIAL_REBUILD = 0b1111;

    private final int typeFlags;
    private final int maximumQueueSize;
    private final int taskEffort;

    ChunkUpdateType(int typeFlags, int maximumQueueSize, int taskEffort) {
        this.typeFlags = typeFlags;
        this.maximumQueueSize = maximumQueueSize;
        this.taskEffort = taskEffort;
    }

    public static ChunkUpdateType getPromotedUpdateType(ChunkUpdateType prev, ChunkUpdateType next) {
        var joined = joinTypes(prev, next);
        if (joined == prev) {
            return null;
        }
        return joined;
    }

    private static ChunkUpdateType joinTypes(ChunkUpdateType a, ChunkUpdateType b) {
        if (a == b) {
            return a;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        return switch (a.typeFlags | b.typeFlags) {
            case TYPE_INITIAL_REBUILD -> INITIAL_BUILD;
            case TYPE_SORT -> SORT;
            case TYPE_REBUILD -> REBUILD;
            case TYPE_SORT | TYPE_REBUILD -> REBUILD_WITH_SORT;
            case TYPE_SORT | TYPE_IMPORTANT -> IMPORTANT_SORT;
            case TYPE_REBUILD | TYPE_IMPORTANT -> IMPORTANT_REBUILD;
            case TYPE_SORT | TYPE_REBUILD | TYPE_IMPORTANT -> IMPORTANT_REBUILD_WITH_SORT;
            default -> throw new IllegalStateException("Unexpected value: " + (a.typeFlags | b.typeFlags));
        };
    }

    public int getMaximumQueueSize() {
        return this.maximumQueueSize;
    }

    public boolean isImportant() {
        return this == IMPORTANT_REBUILD || this == IMPORTANT_SORT || this == IMPORTANT_REBUILD_WITH_SORT;
    }

    public boolean isRebuildWithSort() {
        return this == REBUILD_WITH_SORT || this == IMPORTANT_REBUILD_WITH_SORT;
    }

    public int getTaskEffort() {
        return this.taskEffort;
    }
}
