package net.caffeinemc.mods.sodium.client.render.viewport.frustum;

import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import org.joml.FrustumIntersection;
import org.joml.Vector4f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public final class SimpleFrustum implements Frustum {
    private float nxX, nxY, nxZ, negNxW;
    private float pxX, pxY, pxZ, negPxW;
    private float nyX, nyY, nyZ, negNyW;
    private float pyX, pyY, pyZ, negPyW;
    private float nzX, nzY, nzZ, negNzW;
    private float pzX, pzY, pzZ, negPzW;

    private final FrustumIntersection frustum;

    private static final MethodHandle PLANES_GETTER;
    static {
        try {
            Field field = FrustumIntersection.class.getDeclaredField("planes");
            field.setAccessible(true);
            PLANES_GETTER = MethodHandles.lookup().unreflectGetter(field);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to find planes field in JOML", e);
        }
    }

    public SimpleFrustum(FrustumIntersection frustumIntersection) {
        this.frustum = frustumIntersection;
        Vector4f[] planes;
        try {
            planes = (Vector4f[]) PLANES_GETTER.invokeExact(frustumIntersection);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to access planes field in FrustumIntersection", e);
        }

        this.nxX = planes[0].x;
        this.nxY = planes[0].y;
        this.nxZ = planes[0].z;
        this.pxX = planes[1].x;
        this.pxY = planes[1].y;
        this.pxZ = planes[1].z;
        this.nyX = planes[2].x;
        this.nyY = planes[2].y;
        this.nyZ = planes[2].z;
        this.pyX = planes[3].x;
        this.pyY = planes[3].y;
        this.pyZ = planes[3].z;
        this.nzX = planes[4].x;
        this.nzY = planes[4].y;
        this.nzZ = planes[4].z;
        this.pzX = planes[5].x;
        this.pzY = planes[5].y;
        this.pzZ = planes[5].z;

        final float size = Viewport.CHUNK_SECTION_PADDED_RADIUS;
        this.negNxW = -(planes[0].w + this.nxX * (this.nxX < 0 ? -size : size) +
                this.nxY * (this.nxY < 0 ? -size : size) +
                this.nxZ * (this.nxZ < 0 ? -size : size));
        this.negPxW = -(planes[1].w + this.pxX * (this.pxX < 0 ? -size : size) +
                this.pxY * (this.pxY < 0 ? -size : size) +
                this.pxZ * (this.pxZ < 0 ? -size : size));
        this.negNyW = -(planes[2].w + this.nyX * (this.nyX < 0 ? -size : size) +
                this.nyY * (this.nyY < 0 ? -size : size) +
                this.nyZ * (this.nyZ < 0 ? -size : size));
        this.negPyW = -(planes[3].w + this.pyX * (this.pyX < 0 ? -size : size) +
                this.pyY * (this.pyY < 0 ? -size : size) +
                this.pyZ * (this.pyZ < 0 ? -size : size));
        this.negNzW = -(planes[4].w + this.nzX * (this.nzX < 0 ? -size : size) +
                this.nzY * (this.nzY < 0 ? -size : size) +
                this.nzZ * (this.nzZ < 0 ? -size : size));
        this.negPzW = -(planes[5].w + this.pzX * (this.pzX < 0 ? -size : size) +
                this.pzY * (this.pzY < 0 ? -size : size) +
                this.pzZ * (this.pzZ < 0 ? -size : size));
    }

    public boolean testSection(float x, float y, float z) {
        // Skip far plane checks because it has been ensured by searchDistance and isWithinRenderDistance check in OcclusionCuller
        return this.nxX * x + this.nxY * y + this.nxZ * z >= this.negNxW &&
                this.pxX * x + this.pxY * y + this.pxZ * z >= this.negPxW &&
                this.nyX * x + this.nyY * y + this.nyZ * z >= this.negNyW &&
                this.pyX * x + this.pyY * y + this.pyZ * z >= this.negPyW &&
                this.nzX * x + this.nzY * y + this.nzZ * z >= this.negNzW;
    }

    public boolean testSectionExpanded(float floatOriginX, float floatOriginY, float floatOriginZ, float extend) {
        float minX = floatOriginX - extend;
        float maxX = floatOriginX + extend;
        float minY = floatOriginY - extend;
        float maxY = floatOriginY + extend;
        float minZ = floatOriginZ - extend;
        float maxZ = floatOriginZ + extend;

        return this.nxX * (this.nxX < 0 ? minX : maxX) + this.nxY * (this.nxY < 0 ? minY : maxY) + this.nxZ * (this.nxZ < 0 ? minZ : maxZ) >= this.negNxW &&
                this.pxX * (this.pxX < 0 ? minX : maxX) + this.pxY * (this.pxY < 0 ? minY : maxY) + this.pxZ * (this.pxZ < 0 ? minZ : maxZ) >= this.negPxW &&
                this.nyX * (this.nyX < 0 ? minX : maxX) + this.nyY * (this.nyY < 0 ? minY : maxY) + this.nyZ * (this.nyZ < 0 ? minZ : maxZ) >= this.negNyW &&
                this.pyX * (this.pyX < 0 ? minX : maxX) + this.pyY * (this.pyY < 0 ? minY : maxY) + this.pyZ * (this.pyZ < 0 ? minZ : maxZ) >= this.negPyW &&
                this.nzX * (this.nzX < 0 ? minX : maxX) + this.nzY * (this.nzY < 0 ? minY : maxY) + this.nzZ * (this.nzZ < 0 ? minZ : maxZ) >= this.negNzW &&
                this.pzX * (this.pzX < 0 ? minX : maxX) + this.pzY * (this.pzY < 0 ? minY : maxY) + this.pzZ * (this.pzZ < 0 ? minZ : maxZ) >= this.negPzW;
    }

    @Override
    public boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.frustum.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.frustum.intersectAab(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
