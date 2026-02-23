package net.caffeinemc.mods.sodium.client.render.viewport.frustum;

public interface Frustum {
    boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    int intersectAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    boolean testSection(float floatOriginX, float floatOriginY, float floatOriginZ);

    boolean testSectionExpanded(float floatOriginX, float floatOriginY, float floatOriginZ, float v);
}
