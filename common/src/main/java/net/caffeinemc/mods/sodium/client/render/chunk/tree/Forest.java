package net.caffeinemc.mods.sodium.client.render.chunk.tree;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;

public interface Forest<T extends Tree> {
    /**
     * A tree add method takes a tree and adds the section given by the coordinates to the tree. It returns a tri-state value corresponding to the constants on {@link Tree} indicating whether the section was added, was already present, or was out of bounds.
     *
     * @param <T> the type of tree
     */
    @FunctionalInterface
    interface TreeAddMethod<T extends Tree> {
        int add(T tree, int x, int y, int z);
    }

    default void add(int x, int y, int z) {
        this.add(x, y, z, Tree::add);
    }

    /**
     * Adds a section to the forest with the given add method.
     *
     * @param x         the x coordinate of the section to add
     * @param y         the y coordinate of the section to add
     * @param z         the z coordinate of the section to add
     * @param addMethod the method to use for adding the section to the tree. This allows for different types of trees to be used in the forest.
     * @return true if the section was added to the forest, false if it was already present
     */
    boolean add(int x, int y, int z, TreeAddMethod<T> addMethod);

    default void add(RenderSection section) {
        add(section.getChunkX(), section.getChunkY(), section.getChunkZ());
    }

    int getPresence(int x, int y, int z);

    default boolean isSectionPresent(int x, int y, int z) {
        return this.getPresence(x, y, z) == Tree.PRESENT;
    }
}
