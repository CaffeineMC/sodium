package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad;

import java.util.Arrays;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.api.util.NormI8;

/**
 * Represents a quad for the purposes of translucency sorting. Called TQuad to
 * avoid confusion with other quad classes.
 */
public abstract class TQuad {
    /**
     * The quantization factor with which the normals are quantized such that there
     * are fewer possible unique normals. The factor describes the number of steps
     * in each direction per dimension that the components of the normals can have.
     * It determines the density of the grid on the surface of a unit cube centered
     * at the origin onto which the normals are projected. The normals are snapped
     * to the nearest grid point.
     */
    static final int NORMAL_QUANTIZATION_STEPS = 4;

    private static final float INV_QUANTIZE_EPSILON = 256f;
    public static final float QUANTIZE_EPSILON = 1f / INV_QUANTIZE_EPSILON;

    static {
        // ensure it fits with the fluid renderer epsilon and that it's a power-of-two
        // fraction
        var targetEpsilon = DefaultFluidRenderer.EPSILON * 2.1f;
        if (QUANTIZE_EPSILON <= targetEpsilon && Integer.bitCount((int) INV_QUANTIZE_EPSILON) == 1) {
            throw new RuntimeException("epsilon is invalid: " + QUANTIZE_EPSILON);
        }
    }

    ModelQuadFacing facing;
    final int packedNormal;
    float[] extents;
    float accurateDotProduct;
    float quantizedDotProduct;
    Vector3fc center; // null on aligned quads
    Vector3fc quantizedNormal;
    Vector3fc accurateNormal;

    TQuad(ModelQuadFacing facing, int packedNormal) {
        if (facing.isAligned()) {
            packedNormal = ModelQuadFacing.PACKED_ALIGNED_NORMALS[facing.ordinal()];
        }

        this.facing = facing;
        this.packedNormal = packedNormal;
    }

    protected static boolean isInvalid(int sameVertexMap) {
        return Integer.bitCount(sameVertexMap) > 1;
    }

    int initFull(ChunkVertexEncoder.Vertex[] vertices) {
        var sameVertexMap = this.initExtentsAndCenter(vertices);
        this.initVertexPositions(vertices, sameVertexMap);
        this.initDotProduct();
        return sameVertexMap;
    }

    int initExtentsAndCenter(ChunkVertexEncoder.Vertex[] vertices) {
        float xSum = 0;
        float ySum = 0;
        float zSum = 0;

        // keep track of distinct vertices to compute the center accurately for
        // degenerate quads
        float lastX = vertices[3].x;
        float lastY = vertices[3].y;
        float lastZ = vertices[3].z;
        int sameVertexMap = 0;

        float posXExtent = Float.NEGATIVE_INFINITY;
        float posYExtent = Float.NEGATIVE_INFINITY;
        float posZExtent = Float.NEGATIVE_INFINITY;
        float negXExtent = Float.POSITIVE_INFINITY;
        float negYExtent = Float.POSITIVE_INFINITY;
        float negZExtent = Float.POSITIVE_INFINITY;

        for (int i = 0; i < 4; i++) {
            float x = vertices[i].x;
            float y = vertices[i].y;
            float z = vertices[i].z;

            posXExtent = Math.max(posXExtent, x);
            posYExtent = Math.max(posYExtent, y);
            posZExtent = Math.max(posZExtent, z);
            negXExtent = Math.min(negXExtent, x);
            negYExtent = Math.min(negYExtent, y);
            negZExtent = Math.min(negZExtent, z);

            if (x != lastX || y != lastY || z != lastZ) {
                xSum += x;
                ySum += y;
                zSum += z;
            } else {
                sameVertexMap |= 1 << i;
            }
            if (i != 3) {
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }

        // shrink quad in non-normal directions to prevent intersections caused by
        // epsilon offsets applied by FluidRenderer
        if (this.facing != ModelQuadFacing.POS_X && this.facing != ModelQuadFacing.NEG_X) {
            posXExtent -= QUANTIZE_EPSILON;
            negXExtent += QUANTIZE_EPSILON;
            if (negXExtent > posXExtent) {
                negXExtent = posXExtent;
            }
        }
        if (this.facing != ModelQuadFacing.POS_Y && this.facing != ModelQuadFacing.NEG_Y) {
            posYExtent -= QUANTIZE_EPSILON;
            negYExtent += QUANTIZE_EPSILON;
            if (negYExtent > posYExtent) {
                negYExtent = posYExtent;
            }
        }
        if (this.facing != ModelQuadFacing.POS_Z && this.facing != ModelQuadFacing.NEG_Z) {
            posZExtent -= QUANTIZE_EPSILON;
            negZExtent += QUANTIZE_EPSILON;
            if (negZExtent > posZExtent) {
                negZExtent = posZExtent;
            }
        }

        // POS_X, POS_Y, POS_Z, NEG_X, NEG_Y, NEG_Z
        this.extents = new float[] { posXExtent, posYExtent, posZExtent, negXExtent, negYExtent, negZExtent };

        var uniqueVertexes = 4 - Integer.bitCount(sameVertexMap);
        if ((!this.facing.isAligned() || uniqueVertexes != 4) && uniqueVertexes >= 3) {
            var invUniqueVertexes = 1.0f / uniqueVertexes;
            var centerX = xSum * invUniqueVertexes;
            var centerY = ySum * invUniqueVertexes;
            var centerZ = zSum * invUniqueVertexes;
            this.center = new Vector3f(centerX, centerY, centerZ);
        }

        return sameVertexMap;
    }

    void initDotProduct() {
        if (this.facing.isAligned()) {
            this.accurateDotProduct = getAlignedDotProduct(this.facing, this.extents);
        } else {
            float normX = NormI8.unpackX(this.packedNormal);
            float normY = NormI8.unpackY(this.packedNormal);
            float normZ = NormI8.unpackZ(this.packedNormal);
            this.accurateDotProduct = this.getCenter().dot(normX, normY, normZ);
        }
        this.quantizedDotProduct = this.accurateDotProduct;
    }

    void initVertexPositions(ChunkVertexEncoder.Vertex[] vertices, int sameVertexMap) {
    }

    private static float getAlignedDotProduct(ModelQuadFacing facing, float[] extents) {
        return extents[facing.ordinal()] * facing.getSign();
    }

    public abstract float[] getVertexPositions();

    public ModelQuadFacing getFacing() {
        return this.facing;
    }

    /**
     * Calculates the facing of the quad based on the quantized normal. This updates the dot product to be consistent with the new facing. Since this method computed and allocates the quantized normal, it should be used sparingly and only when the quantized normal is calculated anyway. Additionally, it can modify the facing and not product of the quad which the caller should be aware of.
     *
     * @return the (potentially changed) facing of the quad
     */
    public ModelQuadFacing useQuantizedFacing() {
        if (!this.facing.isAligned()) {
            // quantize the normal, get the new facing and get fix the dot product to match
            this.getQuantizedNormal();
            this.facing = ModelQuadFacing.fromNormal(this.quantizedNormal.x(), this.quantizedNormal.y(), this.quantizedNormal.z());
            if (this.facing.isAligned()) {
                this.quantizedDotProduct = getAlignedDotProduct(this.facing, this.extents);
            } else {
                this.quantizedDotProduct = this.getCenter().dot(this.quantizedNormal);
            }
        }

        return this.facing;
    }

    public float[] getExtents() {
        return this.extents;
    }

    public Vector3fc getCenter() {
        // calculate aligned quad center on demand
        if (this.center == null) {
            this.center = new Vector3f(
                    (this.extents[0] + this.extents[3]) / 2,
                    (this.extents[1] + this.extents[4]) / 2,
                    (this.extents[2] + this.extents[5]) / 2);
        }
        return this.center;
    }

    public float getAccurateDotProduct() {
        return this.accurateDotProduct;
    }

    public float getQuantizedDotProduct() {
        return this.quantizedDotProduct;
    }

    public int getPackedNormal() {
        return this.packedNormal;
    }

    public Vector3fc getQuantizedNormal() {
        if (this.quantizedNormal == null) {
            if (this.facing.isAligned()) {
                this.quantizedNormal = this.facing.getAlignedNormal();
            } else {
                this.computeQuantizedNormal();
            }
        }
        return this.quantizedNormal;
    }

    public Vector3fc getAccurateNormal() {
        if (this.facing.isAligned()) {
            return this.facing.getAlignedNormal();
        } else {
            if (this.accurateNormal == null) {
                this.accurateNormal = new Vector3f(
                        NormI8.unpackX(this.packedNormal),
                        NormI8.unpackY(this.packedNormal),
                        NormI8.unpackZ(this.packedNormal));
            }
            return this.accurateNormal;
        }
    }

    private void computeQuantizedNormal() {
        float normX = NormI8.unpackX(this.packedNormal);
        float normY = NormI8.unpackY(this.packedNormal);
        float normZ = NormI8.unpackZ(this.packedNormal);

        // normalize onto the surface of a cube by dividing by the length of the longest
        // component
        float infNormLength = Math.max(Math.abs(normX), Math.max(Math.abs(normY), Math.abs(normZ)));
        if (infNormLength != 0 && infNormLength != 1) {
            normX /= infNormLength;
            normY /= infNormLength;
            normZ /= infNormLength;
        }

        // quantize the coordinates on the surface of the cube.
        // in each axis the number of values is 2 * QUANTIZATION_FACTOR + 1.
        // the total number of normals is the number of points on that cube's surface.
        var normal = new Vector3f(
                (int) (normX * NORMAL_QUANTIZATION_STEPS),
                (int) (normY * NORMAL_QUANTIZATION_STEPS),
                (int) (normZ * NORMAL_QUANTIZATION_STEPS));
        normal.normalize();
        this.quantizedNormal = normal;
    }

    public int getQuadHash() {
        // the hash code needs to be particularly collision resistant
        int result = 1;
        result = 31 * result + Arrays.hashCode(this.extents);
        if (this.facing.isAligned()) {
            result = 31 * result + this.facing.hashCode();
        } else {
            result = 31 * result + this.packedNormal;
        }
        result = 31 * result + Float.hashCode(this.quantizedDotProduct);
        return result;
    }

    public boolean extentsEqual(float[] other) {
        return extentsEqual(this.extents, other);
    }

    public static boolean extentsEqual(float[] a, float[] b) {
        for (int i = 0; i < 6; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean extentsIntersect(float[] extentsA, float[] extentsB) {
        for (int axis = 0; axis < 3; axis++) {
            var opposite = axis + 3;

            if (extentsA[axis] <= extentsB[opposite]
                    || extentsB[axis] <= extentsA[opposite]) {
                return false;
            }
        }
        return true;
    }

    public static boolean extentsIntersect(TQuad a, TQuad b) {
        return extentsIntersect(a.extents, b.extents);
    }
}
