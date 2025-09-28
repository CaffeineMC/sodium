package net.caffeinemc.mods.sodium.client.render.chunk.tree;

public interface RemovableForest<T extends RemovableTree> extends TraversableForest<T> {
    void remove(int x, int y, int z);
}
