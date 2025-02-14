package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class FullTQuad extends RegularTQuad {
    private final ChunkVertexEncoder.Vertex[] vertices;
    private boolean normalIsVeryAccurate = false;

    // TODO: when vertexes are modified we need to re-calculate at least the extent, centroid, invalidate vertex positions (but not the normal or dot product)
    FullTQuad(ModelQuadFacing facing, float[] extents, Vector3fc center, ChunkVertexEncoder.Vertex[] vertices, int packedNormal) {
        super(facing, extents, null, center, packedNormal);

        // deep copy the vertices since the caller may modify them
        this.vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
        for (int i = 0; i < 4; i++) {
            var newVertex = this.vertices[i];
            var oldVertex = vertices[i];
            newVertex.x = oldVertex.x;
            newVertex.y = oldVertex.y;
            newVertex.z = oldVertex.z;
            newVertex.color = oldVertex.color;
            newVertex.ao = oldVertex.ao;
            newVertex.u = oldVertex.u;
            newVertex.v = oldVertex.v;
            newVertex.light = oldVertex.light;
        }
    }

    public FullTQuad(FullTQuad other) {
        this(other.facing, other.extents, other.center, other.vertices, other.packedNormal);
        this.normalIsVeryAccurate = other.normalIsVeryAccurate;
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

    public static TQuad fromAligned(ModelQuadFacing facing, float[] extents, Vector3fc center, ChunkVertexEncoder.Vertex[] vertices) {
        return new FullTQuad(facing, extents, center, vertices, ModelQuadFacing.PACKED_ALIGNED_NORMALS[facing.ordinal()]);
    }

    public static TQuad fromUnaligned(ModelQuadFacing facing, float[] extents, Vector3fc center, ChunkVertexEncoder.Vertex[] vertices, int packedNormal) {
        return new FullTQuad(facing, extents, center, vertices, packedNormal);
    }
}
