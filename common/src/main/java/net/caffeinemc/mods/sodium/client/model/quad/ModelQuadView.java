package net.caffeinemc.mods.sodium.client.model.quad;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Provides a read-only view of a model quad. For mutable access to a model quad, see {@link ModelQuadViewMutable}.
 */
public interface ModelQuadView {
    /**
     * @return The x-position of the vertex at index {@param idx}
     */
    float sodium$getX(int idx);

    /**
     * @return The y-position of the vertex at index {@param idx}
     */
    float sodium$getY(int idx);

    /**
     * @return The z-position of the vertex at index {@param idx}
     */
    float sodium$getZ(int idx);

    /**
     * @return The integer-encoded color of the vertex at index {@param idx}
     */
    int sodium$getColor(int idx);

    /**
     * @return The texture x-coordinate for the vertex at index {@param idx}
     */
    float sodium$getTexU(int idx);

    /**
     * @return The texture y-coordinate for the vertex at index {@param idx}
     */
    float sodium$getTexV(int idx);

    /**
     * @return The packed normal set for the vertex at index {@param idx}.
     */
    int sodium$getVertexNormal(int idx);

    /**
     * @return The computed normal.
     */
    int sodium$getFaceNormal();

    /**
     * @return The packed light set for the vertex at index {@param idx}.
     */
    int sodium$getLight(int idx);

    /**
     * @return The integer bit flags containing the {@link ModelQuadFlags} for this quad
     */
    int sodium$getFlags();

    /**
     * @return The color index of this quad.
     */
    int sodium$getColorIndex();

    /**
     * @return The sprite texture used by this quad, or null if none is attached
     */
    TextureAtlasSprite sodium$getSprite();
    
    /**
     * @return The face used by this quad for lighting effects
     */
    Direction sodium$getLightFace();

    default boolean sodium$hasColor() {
        return this.sodium$getColorIndex() != -1;
    }

    default int sodium$calculateNormal() {
        final float x0 = this.sodium$getX(0);
        final float y0 = this.sodium$getY(0);
        final float z0 = this.sodium$getZ(0);

        final float x1 = this.sodium$getX(1);
        final float y1 = this.sodium$getY(1);
        final float z1 = this.sodium$getZ(1);

        final float x2 = this.sodium$getX(2);
        final float y2 = this.sodium$getY(2);
        final float z2 = this.sodium$getZ(2);

        final float x3 = this.sodium$getX(3);
        final float y3 = this.sodium$getY(3);
        final float z3 = this.sodium$getZ(3);

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        // normalize by length for the packed normal
        float length = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);
        if (length != 0.0 && length != 1.0) {
            normX /= length;
            normY /= length;
            normZ /= length;
        }

        return NormI8.pack(normX, normY, normZ);
    }

    /**
     * Returns the most accurate normal value for this vertex.
     * @param i The vertex index.
     * @return the per-vertex normal if it is set, otherwise the face normal.
     */
    default int sodium$getAccurateNormal(int i) {
        int normal = this.sodium$getVertexNormal(i);

        return normal == 0 ? this.sodium$getFaceNormal() : normal;
    }
}
