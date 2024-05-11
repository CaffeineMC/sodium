package net.caffeinemc.mods.sodium.mixin.features.render.immediate.matrix_stack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
    @Shadow
    VertexConsumer normal(float x, float y, float z);

    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        float xt = MatrixHelper.transformPositionX(matrix, x, y, z);
        float yt = MatrixHelper.transformPositionY(matrix, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matrix, x, y, z);

        return this.vertex(xt, yt, zt);
    }

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer normal(PoseStack.Pose matrix, float x, float y, float z) {
        float xt = MatrixHelper.transformNormalX(matrix.normal(), x, y, z);
        float yt = MatrixHelper.transformNormalY(matrix.normal(), x, y, z);
        float zt = MatrixHelper.transformNormalZ(matrix.normal(), x, y, z);

        return this.normal(xt, yt, zt);
    }
}
