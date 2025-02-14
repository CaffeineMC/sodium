package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.quad;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.joml.Vector3fc;

public class RegularTQuad extends TQuad {
    float[] vertexPositions;

    RegularTQuad(ModelQuadFacing facing, float[] extents, float[] vertexPositions, Vector3fc center, int packedNormal) {
        super(facing, extents, center, packedNormal);
        this.vertexPositions = vertexPositions;
    }

    public float[] getVertexPositions() {
        // calculate vertex positions from extents if there's no cached value
        // (we don't want to be preemptively collecting vertex positions for all aligned quads)
        if (this.vertexPositions == null) {
            this.vertexPositions = new float[12];

            var facingAxis = this.facing.getAxis();
            var xRange = facingAxis == 0 ? 0 : 3;
            var yRange = facingAxis == 1 ? 0 : 3;
            var zRange = facingAxis == 2 ? 0 : 3;

            var itemIndex = 0;
            for (int x = 0; x <= xRange; x += 3) {
                for (int y = 0; y <= yRange; y += 3) {
                    for (int z = 0; z <= zRange; z += 3) {
                        this.vertexPositions[itemIndex++] = this.extents[x];
                        this.vertexPositions[itemIndex++] = this.extents[y + 1];
                        this.vertexPositions[itemIndex++] = this.extents[z + 2];
                    }
                }
            }
        }
        return this.vertexPositions;
    }

    public static TQuad fromAligned(ModelQuadFacing facing, float[] extents, float[] vertexPositions, Vector3fc center) {
        return new RegularTQuad(facing, extents, vertexPositions, center, ModelQuadFacing.PACKED_ALIGNED_NORMALS[facing.ordinal()]);
    }

    public static TQuad fromUnaligned(ModelQuadFacing facing, float[] extents, float[] vertexPositions, Vector3fc center, int packedNormal) {
        return new RegularTQuad(facing, extents, vertexPositions, center, packedNormal);
    }
}
