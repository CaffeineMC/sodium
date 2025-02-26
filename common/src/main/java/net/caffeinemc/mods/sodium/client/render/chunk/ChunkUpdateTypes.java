package net.caffeinemc.mods.sodium.client.render.chunk;

/**
 * Important: Whether the task is scheduled immediately after its creation. Otherwise, they're scheduled through
 * asynchronous culling that collects non-important tasks. Defer mode: For important tasks, how fast they are going to
 * be executed. One or zero frame deferral only allows one or zero frames to pass before the frame blocks on the task.
 * Always deferral allows the task to be deferred indefinitely, but if it's important it will still be put to the front
 * of the queue.
 */
public class ChunkUpdateTypes {
    public static final int SORT = 0b001;
    public static final int REBUILD = 0b010;
    public static final int IMPORTANT = 0b100;
    public static final int INITIAL_BUILD = 0b1000;

    public static int join(int from, int to) {
        return from | to;
    }

    public static boolean isSort(int type) {
        return (type & SORT) != 0;
    }

    public static boolean isRebuild(int type) {
        return (type & REBUILD) != 0;
    }

    public static boolean isImportant(int type) {
        return (type & IMPORTANT) != 0;
    }

    public static boolean isInitialBuild(int type) {
        return (type & INITIAL_BUILD) != 0;
    }

    public static boolean isRebuildWithSort(int type) {
        return (isRebuild(type) || isInitialBuild(type)) && isSort(type);
    }

    public static DeferMode getDeferMode(int type, DeferMode importantRebuildDeferMode) {
        if (isImportant(type)) {
            if (isRebuild(type)) {
                return importantRebuildDeferMode;
            } else { // implies important sort task
                return DeferMode.ZERO_FRAMES;
            }
        } else {
            return DeferMode.ALWAYS;
        }
    }

    public static int getPriorityValue(int type) {
        if (isInitialBuild(type)) {
            return 0;
        }
        if (isRebuild(type)) {
            return 1;
        }
        return 2; // sort
    }
}
