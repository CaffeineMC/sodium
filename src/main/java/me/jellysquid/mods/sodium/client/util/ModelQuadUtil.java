package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

/**
 * Provides some utilities and constants for interacting with vanilla's model quad vertex format.
 *
 * This is the current vertex format used by Minecraft for chunk meshes and model quads. Internally, it uses integer
 * arrays for store baked quad data, and as such the following table provides both the byte and int indices.
 *
 * Byte Index    Integer Index             Name                 Format                 Fields
 * 0 ..11        0..2                      Position             3 floats               x, y, z
 * 12..15        3                         Color                4 unsigned bytes       a, r, g, b
 * 16..23        4..5                      Block Texture        2 floats               u, v
 * 24..27        6                         Light Texture        2 shorts               u, v
 * 28..30        7                         Normal               3 unsigned bytes       x, y, z
 * 31                                      Padding              1 byte
 */
public class ModelQuadUtil {
    // Integer indices for vertex attributes, useful for accessing baked quad data
    public static final int POSITION_INDEX = 0,
            COLOR_INDEX = 3,
            TEXTURE_INDEX = 4,
            LIGHT_INDEX = 6,
            NORMAL_INDEX = 7;

    // Size of vertex format in 4-byte integers
    public static final int VERTEX_SIZE = 8;

    // Cached array of normals for every facing to avoid expensive computation
    static final int[] NORMALS = new int[DirectionUtil.ALL_DIRECTIONS.length];

    static {
        for (int i = 0; i < NORMALS.length; i++) {
            NORMALS[i] = NormI8.pack(DirectionUtil.ALL_DIRECTIONS[i].getUnitVector());
        }
    }

    /**
     * Returns the normal vector for a model quad with the given {@param facing}.
     */
    public static int getFacingNormal(Direction facing) {
        return NORMALS[facing.ordinal()];
    }

    /**
     * @param vertexIndex The index of the vertex to access
     * @return The starting offset of the vertex's attributes
     */
    public static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    public static ModelQuadFacing findClosestFacing(ModelQuadView quad) {
        Vector3f pos0 = new Vector3f(quad.getX(0), quad.getY(0), quad.getZ(0));
        Vector3f dist10 = new Vector3f(quad.getX(1), quad.getY(1), quad.getZ(1)).sub(pos0);
        Vector3f dist30 = new Vector3f(quad.getX(3), quad.getY(3), quad.getZ(3)).sub(pos0);
        Vector3f normal = dist10.cross(dist30).normalize();

        if (!normal.isFinite()) {
            return ModelQuadFacing.UNASSIGNED;
        }

        float maxDot = 0;
        Direction closestFace = null;
        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            float dot = normal.dot(face.getUnitVector());
            if (dot > maxDot) {
                maxDot = dot;
                closestFace = face;
            }
        }

        if (closestFace != null && MathHelper.approximatelyEquals(maxDot, 1)) {
            return ModelQuadFacing.fromDirection(closestFace);
        }

        return ModelQuadFacing.UNASSIGNED;
    }
}
