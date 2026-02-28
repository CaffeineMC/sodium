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

        nxX = planes[0].x;
        nxY = planes[0].y;
        nxZ = planes[0].z;
        pxX = planes[1].x;
        pxY = planes[1].y;
        pxZ = planes[1].z;
        nyX = planes[2].x;
        nyY = planes[2].y;
        nyZ = planes[2].z;
        pyX = planes[3].x;
        pyY = planes[3].y;
        pyZ = planes[3].z;
        nzX = planes[4].x;
        nzY = planes[4].y;
        nzZ = planes[4].z;
        pzX = planes[5].x;
        pzY = planes[5].y;
        pzZ = planes[5].z;

        final float size = Viewport.CHUNK_SECTION_PADDED_RADIUS;
        negNxW = -(planes[0].w + nxX * (nxX < 0 ? -size : size) +
                nxY * (nxY < 0 ? -size : size) +
                nxZ * (nxZ < 0 ? -size : size));
        negPxW = -(planes[1].w + pxX * (pxX < 0 ? -size : size) +
                pxY * (pxY < 0 ? -size : size) +
                pxZ * (pxZ < 0 ? -size : size));
        negNyW = -(planes[2].w + nyX * (nyX < 0 ? -size : size) +
                nyY * (nyY < 0 ? -size : size) +
                nyZ * (nyZ < 0 ? -size : size));
        negPyW = -(planes[3].w + pyX * (pyX < 0 ? -size : size) +
                pyY * (pyY < 0 ? -size : size) +
                pyZ * (pyZ < 0 ? -size : size));
        negNzW = -(planes[4].w + nzX * (nzX < 0 ? -size : size) +
                nzY * (nzY < 0 ? -size : size) +
                nzZ * (nzZ < 0 ? -size : size));
        negPzW = -(planes[5].w + pzX * (pzX < 0 ? -size : size) +
                pzY * (pzY < 0 ? -size : size) +
                pzZ * (pzZ < 0 ? -size : size));
    }

    public boolean testSection(float x, float y, float z) {
        // Skip far plane checks because it has been ensured by searchDistance and isWithinRenderDistance check in OcclusionCuller
        return nxX * x + nxY * y + nxZ * z >= negNxW &&
                pxX * x + pxY * y + pxZ * z >= negPxW &&
                nyX * x + nyY * y + nyZ * z >= negNyW &&
                pyX * x + pyY * y + pyZ * z >= negPyW &&
                nzX * x + nzY * y + nzZ * z >= negNzW;
    }

    public boolean testSectionExpanded(float floatOriginX, float floatOriginY, float floatOriginZ, float extend) {
        float minX = floatOriginX - extend;
        float maxX = floatOriginX + extend;
        float minY = floatOriginY - extend;
        float maxY = floatOriginY + extend;
        float minZ = floatOriginZ - extend;
        float maxZ = floatOriginZ + extend;

        return nxX * (nxX < 0 ? minX : maxX) + nxY * (nxY < 0 ? minY : maxY) + nxZ * (nxZ < 0 ? minZ : maxZ) >= negNxW &&
                pxX * (pxX < 0 ? minX : maxX) + pxY * (pxY < 0 ? minY : maxY) + pxZ * (pxZ < 0 ? minZ : maxZ) >= negPxW &&
                nyX * (nyX < 0 ? minX : maxX) + nyY * (nyY < 0 ? minY : maxY) + nyZ * (nyZ < 0 ? minZ : maxZ) >= negNyW &&
                pyX * (pyX < 0 ? minX : maxX) + pyY * (pyY < 0 ? minY : maxY) + pyZ * (pyZ < 0 ? minZ : maxZ) >= negPyW &&
                nzX * (nzX < 0 ? minX : maxX) + nzY * (nzY < 0 ? minY : maxY) + nzZ * (nzZ < 0 ? minZ : maxZ) >= negNzW &&
                pzX * (pzX < 0 ? minX : maxX) + pzY * (pzY < 0 ? minY : maxY) + pzZ * (pzZ < 0 ? minZ : maxZ) >= negPzW;
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
