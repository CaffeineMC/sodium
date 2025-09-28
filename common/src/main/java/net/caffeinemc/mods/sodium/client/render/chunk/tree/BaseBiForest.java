package net.caffeinemc.mods.sodium.client.render.chunk.tree;

import net.minecraft.world.level.Level;

public abstract class BaseBiForest<T extends Tree> extends BaseForest<T> {
    private static final int SECONDARY_TREE_OFFSET_XZ = 4;

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
    public boolean add(int x, int y, int z, TreeAddMethod<T> addMethod) {
        var result = addMethod.add(this.mainTree, x, y, z);
        if (result != Tree.OUT_OF_BOUNDS) {
            return result == Tree.NOT_PRESENT;
        }

        if (this.secondaryTree == null) {
            this.secondaryTree = this.makeSecondaryTree();
        }
        return addMethod.add(this.secondaryTree, x, y, z) == Tree.NOT_PRESENT;
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
        if (buildDistance / 16.0f > 64.0f) {
            return false;
        }

        return level.getHeight() >> 4 <= 64;
    }
}
