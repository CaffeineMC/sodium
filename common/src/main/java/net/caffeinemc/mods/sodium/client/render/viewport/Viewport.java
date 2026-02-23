package net.caffeinemc.mods.sodium.client.render.viewport;

import net.caffeinemc.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.joml.Vector3d;

public final class Viewport {
    private final Frustum frustum;
    private final CameraTransform transform;

    private final SectionPos sectionCoords;
    private final BlockPos blockCoords;

    public Viewport(Frustum frustum, Vector3d position) {
        this.frustum = frustum;
        this.transform = new CameraTransform(position.x, position.y, position.z);

        this.sectionCoords = SectionPos.of(
                SectionPos.posToSectionCoord(position.x),
                SectionPos.posToSectionCoord(position.y),
                SectionPos.posToSectionCoord(position.z)
        );

        this.blockCoords = BlockPos.containing(position.x, position.y, position.z);
    }

    public boolean isBoxVisible(int intOriginX, int intOriginY, int intOriginZ) {
        float floatOriginX = (intOriginX - this.transform.intX) - this.transform.fracX;
        float floatOriginY = (intOriginY - this.transform.intY) - this.transform.fracY;
        float floatOriginZ = (intOriginZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testSection(floatOriginX, floatOriginY, floatOriginZ);
    }

    public boolean isBoxVisibleLooser(int intOriginX, int intOriginY, int intOriginZ) {
        float floatOriginX = (intOriginX - this.transform.intX) - this.transform.fracX;
        float floatOriginY = (intOriginY - this.transform.intY) - this.transform.fracY;
        float floatOriginZ = (intOriginZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testSectionExpanded(floatOriginX, floatOriginY, floatOriginZ, 1.0625f);
    }

    public boolean isBoxVisibleDirect(float floatOriginX, float floatOriginY, float floatOriginZ, float floatSize) {
        return this.frustum.testAab(
                floatOriginX - floatSize,
                floatOriginY - floatSize,
                floatOriginZ - floatSize,

                floatOriginX + floatSize,
                floatOriginY + floatSize,
                floatOriginZ + floatSize
        );
    }

    public int getBoxIntersectionDirect(float floatOriginX, float floatOriginY, float floatOriginZ, float floatSize) {
        return this.frustum.intersectAab(
                floatOriginX - floatSize,
                floatOriginY - floatSize,
                floatOriginZ - floatSize,

                floatOriginX + floatSize,
                floatOriginY + floatSize,
                floatOriginZ + floatSize
        );
    }

    public CameraTransform getTransform() {
        return this.transform;
    }

    public SectionPos getChunkCoord() {
        return this.sectionCoords;
    }

    public BlockPos getBlockCoord() {
        return this.blockCoords;
    }
}
