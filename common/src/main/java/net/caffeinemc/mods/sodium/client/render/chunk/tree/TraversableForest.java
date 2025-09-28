package net.caffeinemc.mods.sodium.client.render.chunk.tree;

import net.caffeinemc.mods.sodium.client.render.chunk.lists.CoordinateSectionVisitor;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.world.level.Level;

public interface TraversableForest<T extends TraversableTree> extends Forest<T> {
    void prepareForTraversal();

    void traverse(CoordinateSectionVisitor visitor, Viewport viewport, float distanceLimit);

    default boolean addPatch(int x, int y, int z) {
        return this.add(x, y, z, TraversableTree::addPatch);
    }

    static TraversableForest<TraversableTree> createTraversableForest(int baseOffsetX, int baseOffsetY, int baseOffsetZ, float buildDistance, Level level) {
        if (BaseBiForest.checkApplicable(buildDistance, level)) {
            return new TraversableBiForest(baseOffsetX, baseOffsetY, baseOffsetZ, buildDistance);
        }

        return new TraversableMultiForest(baseOffsetX, baseOffsetY, baseOffsetZ, buildDistance);
    }
}
