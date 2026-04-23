package net.caffeinemc.mods.sodium.client.render.chunk;

/**
 * Bitmasks representing the types of invalidation that can occur from application of a new section info to a render section.
 */
public class SectionInfoChange {
    public static final int NONE = 0;

    /**
     * Invalidation of the occlusion graph state.
     */
    public static final int GRAPH = 1 << 0;

    /**
     * Invalidation of the current render lists.
     */
    public static final int RENDER_LIST = 1 << 1;
}
