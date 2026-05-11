package net.caffeinemc.mods.sodium.client.render.immediate.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class BakedModelEncoder {
    private static final boolean USE_COLOR_MULTIPLICATION = PlatformRuntimeInformation.getInstance().usesBakedQuadColorMultiplication();

    /**
     * Encodes the given quad into the provided writer, applying the transformations and combining the data from {@code quad} and {@code instance}, where {@code instance} is modified dynamically and {@code quad} comes from the baked model.
     *
     * @param writer The writer to write the vertex data into.
     * @param matrices The current transformation matrices to apply to the vertex data.
     * @param quad The quad to encode, providing the base vertex data.
     * @param instance The instance providing dynamic data such as color and light, which may also modify the base vertex data from the quad.
     */
    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, BakedQuadView quad, QuadInstance instance) {
        Matrix3f matNormal = matrices.normal();
        Matrix4f matPosition = matrices.pose();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * EntityVertex.STRIDE);
            long ptr = buffer;

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // take the base quad's material's emission and apply it to the instance's light
                int newLight = instance.getLightCoordsWithEmission(i, quad.getLightEmission());

                //  NeoForge patches the default VertexConsumer.putBakedQuad to do ARGB.multiply(instance.getColor(vertex), quad.bakedColors().color(vertex)), but Sodium short-circuits that path via BufferBuilderMixin, so the multiplication is lost. Blocks that encode their tint only in element.color(...) (XyCraft ores) lose all color, and blocks combining a BlockTintSource with a baked color get only one factor applied.
                //  The platform flag is needed because Fabric's default implementation does not perform this multiplication.
                int color = instance.getColor(i);
                if (USE_COLOR_MULTIPLICATION) {
                    color = ColorMixer.mulComponentWise(color, quad.getColor(i));
                }
                int newColor = ColorARGB.toABGR(color);

                // The packed transformed normal vector
                int normal = MatrixHelper.transformNormal(matNormal, matrices.trustedNormals, quad.getAccurateNormal(i));

                // The transformed position vector
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                EntityVertex.write(ptr, xt, yt, zt, newColor, quad.getTexU(i), quad.getTexV(i), instance.overlayCoords(), newLight, normal);
                ptr += EntityVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, EntityVertex.FORMAT);
        }
    }
}
