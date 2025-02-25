package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class FullTQuad extends RegularTQuad {
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private int sameVertexMap;
    private boolean normalIsVeryAccurate = false;
    private int updateBufferIndex = -1;

    FullTQuad(ModelQuadFacing facing, int packedNormal) {
        super(facing, packedNormal);
    }

    public static FullTQuad fromVertices(ChunkVertexEncoder.Vertex[] vertices, ModelQuadFacing facing, int packedNormal) {
        var quad = new FullTQuad(facing, packedNormal);
        quad.sameVertexMap = quad.initExtentsAndCenter(vertices);

        quad.initDotProduct();
        quad.initVertices(vertices);

        return quad;
    }

    private void initVertices(ChunkVertexEncoder.Vertex[] vertices) {
        // deep copy the vertices since the caller may modify them
        for (int i = 0; i < 4; i++) {
            var newVertex = this.vertices[i];
            var oldVertex = vertices[i];
            ChunkVertexEncoder.Vertex.copyVertexTo(oldVertex, newVertex);
        }
    }

    public static FullTQuad splittingCopy(FullTQuad quad) {
        var newQuad = new FullTQuad(quad.facing, quad.packedNormal);
        newQuad.initVertices(quad.vertices);

        newQuad.extents = quad.extents;
        newQuad.accurateDotProduct = quad.accurateDotProduct;
        newQuad.quantizedDotProduct = quad.quantizedDotProduct;

        newQuad.center = quad.center;
        newQuad.quantizedNormal = quad.quantizedNormal;
        newQuad.accurateNormal = quad.accurateNormal;

        newQuad.normalIsVeryAccurate = quad.normalIsVeryAccurate;

        return newQuad;
    }

    public void updateSplitQuadAfterVertexModification() {
        this.sameVertexMap = this.initExtentsAndCenter(this.vertices);

        // invalidate vertex positions after modification of the vertices
        this.vertexPositions = null;

        // no need to update dot product since splitting a quad doesn't change its normal or dot product
    }

    public boolean isInvalid() {
        return isInvalid(this.sameVertexMap);
    }

    public int getUniqueVertexMap() {
        return (~this.sameVertexMap) & 0b1111;
    }

    public int getSameVertexMap() {
        return this.sameVertexMap;
    }

    public boolean hasUpdateBufferIndex() {
        return this.updateBufferIndex != -1;
    }

    public int getUpdateBufferIndex() {
        return this.updateBufferIndex;
    }

    public void setUpdateBufferIndex(int index) {
        this.updateBufferIndex = index;
    }

    @Override
    public float[] getVertexPositions() {
        if (this.vertexPositions == null) {
            this.vertexPositions = new float[12];

            for (int i = 0; i < 4; i++) {
                this.vertexPositions[i * 3] = this.vertices[i].x;
                this.vertexPositions[i * 3 + 1] = this.vertices[i].y;
                this.vertexPositions[i * 3 + 2] = this.vertices[i].z;
            }
        }

        return this.vertexPositions;
    }

    public Vector3fc getVeryAccurateNormal() {
        if (this.facing.isAligned()) {
            return this.facing.getAlignedNormal();
        } else {
            if (!this.normalIsVeryAccurate) {
                final float x0 = this.vertices[0].x;
                final float y0 = this.vertices[0].y;
                final float z0 = this.vertices[0].z;

                final float x1 = this.vertices[1].x;
                final float y1 = this.vertices[1].y;
                final float z1 = this.vertices[1].z;

                final float x2 = this.vertices[2].x;
                final float y2 = this.vertices[2].y;
                final float z2 = this.vertices[2].z;

                final float x3 = this.vertices[3].x;
                final float y3 = this.vertices[3].y;
                final float z3 = this.vertices[3].z;

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
                // TODO: normalization necessary?
                float length = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);
                if (length != 0.0 && length != 1.0) {
                    normX /= length;
                    normY /= length;
                    normZ /= length;
                }

                this.accurateNormal = new Vector3f(normX, normY, normZ);
                this.accurateDotProduct = this.accurateNormal.dot(this.center);
                this.normalIsVeryAccurate = true;
            }
        }
        return this.accurateNormal;
    }

    public ChunkVertexEncoder.Vertex[] getVertices() {
        return this.vertices;
    }
}
