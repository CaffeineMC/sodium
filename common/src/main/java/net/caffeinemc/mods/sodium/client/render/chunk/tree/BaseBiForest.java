package net.caffeinemc.mods.sodium.client.render.chunk.tree;

import net.minecraft.world.level.Level;

public abstract class BaseBiForest<T extends Tree> extends BaseForest<T> {
    private static final int SECONDARY_TREE_OFFSET_XZ = 4;
    private static final int MAX_BUILD_DISTANCE = 16 * 65 / 2; // radius of 32 chunk render distance

    protected final T mainTree;
    protected T secondaryTree;

    public BaseBiForest(int baseOffsetX,int baseOffsetY, int baseOffsetZ, float buildDistance) {
        super(baseOffsetX, baseOffsetY, baseOffsetZ, buildDistance);

        this.mainTree = this.makeTree(this.baseOffsetX, this.baseOffsetY, this.baseOffsetZ);
    }

    protected T makeSecondaryTree() {
        // offset diagonally to fully encompass the required 65x65 area
        return this.makeTree(
                this.baseOffsetX + SECONDARY_TREE_OFFSET_XZ,
                this.baseOffsetY,
                this.baseOffsetZ + SECONDARY_TREE_OFFSET_XZ);
    }

    @Override
    public void add(int x, int y, int z) {
        if (this.mainTree.add(x, y, z)) {
            return;
        }

        if (this.secondaryTree == null) {
            this.secondaryTree = this.makeSecondaryTree();
        }
        this.secondaryTree.add(x, y, z);
    }

    @Override
    public int getPresence(int x, int y, int z) {
        var result = this.mainTree.getPresence(x, y, z);
        if (result != Tree.OUT_OF_BOUNDS) {
            return result;
        }

        if (this.secondaryTree != null) {
            return this.secondaryTree.getPresence(x, y, z);
        }
        return Tree.OUT_OF_BOUNDS;
    }

    public static boolean checkApplicable(float buildDistance, Level level) {
        int buildDistanceInt = (int) Math.ceil(buildDistance);
        if (buildDistanceInt > MAX_BUILD_DISTANCE) {
            return false;
        }

        return level.getHeight() >> 4 <= 64;
    }
}
