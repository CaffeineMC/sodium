package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;

/**
 * The result of a BSP building operation. Building a BSP returns the root node
 * along with the partition planes that need to be added to the trigger system.
 */
public class BSPResult extends GeometryPlanes {
    private BSPNode rootNode;
    private UpdatedQuadIndexes updatedQuadIndexes;

    public BSPNode getRootNode() {
        return this.rootNode;
    }

    public void setRootNode(BSPNode rootNode) {
        this.rootNode = rootNode;
    }

    public UpdatedQuadIndexes getUpdatedQuadIndexes() {
        return this.updatedQuadIndexes;
    }

    public UpdatedQuadIndexes ensureUpdatedQuadIndexes() {
        if (this.updatedQuadIndexes == null) {
            this.updatedQuadIndexes = new UpdatedQuadIndexes();
        }
        return this.updatedQuadIndexes;
    }

    public int getAppendedQuadCount() {
        if (this.updatedQuadIndexes == null) {
            return 0;
        }

        return this.updatedQuadIndexes.getAddedQuadCount();
    }

    public static class UpdatedQuadIndexes extends IntArrayList {
        private int addedQuadCount;

        public void addModifiedQuadIndex(int index) {
            this.add(index);
        }

        public void addAppendedQuadIndex(int index) {
            this.add(index);
            this.addedQuadCount++;
        }

        public int getAddedQuadCount() {
            return this.addedQuadCount;
        }
    }
}
