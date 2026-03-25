package net.caffeinemc.mods.sodium.client.render.chunk.lists;

import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.util.Mth;

public abstract class AbstractSectionVisitor implements OcclusionCuller.GraphOcclusionVisitor {
    protected final boolean isFrustumTested;
    protected final int baseOffsetX, baseOffsetY, baseOffsetZ;

    protected final int cameraX, cameraY, cameraZ;

    public AbstractSectionVisitor(Viewport viewport, float buildDistance, boolean frustumTested) {
        this.isFrustumTested = frustumTested;
        var offsetDistance = Mth.ceil(buildDistance / 16.0f) + 1;

        // the offset applied to section coordinates to encode their position in the octree
        var sectionPos = viewport.getChunkCoord();
        var cameraSectionX = sectionPos.getX();
        var cameraSectionY = sectionPos.getY();
        var cameraSectionZ = sectionPos.getZ();
        this.baseOffsetX = cameraSectionX - offsetDistance;
        this.baseOffsetY = cameraSectionY - offsetDistance;
        this.baseOffsetZ = cameraSectionZ - offsetDistance;

        if (frustumTested) {
            var blockPos = viewport.getBlockCoord();
            this.cameraX = blockPos.getX();
            this.cameraY = blockPos.getY();
            this.cameraZ = blockPos.getZ();
        } else {
            this.cameraX = (cameraSectionX << 4);
            this.cameraY = (cameraSectionY << 4);
            this.cameraZ = (cameraSectionZ << 4);
        }
    }
}
