package net.caffeinemc.mods.sodium.mixin.features.render.entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelPart.class)
public class ModelPartMixin {

    @Unique
    private static final Quaternionf TEMP_QUAT = new Quaternionf();

    @Shadow
    public float x;
    @Shadow
    public float y;
    @Shadow
    public float z;

    @Shadow
    public float yRot;
    @Shadow
    public float xRot;
    @Shadow
    public float zRot;

    @WrapWithCondition(
            method = "translateAndRotate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"
            )
    )
    public boolean skipEmptyTranslation(PoseStack instance, float xo, float yo, float zo) {
        return xo != 0.0F || yo != 0.0F || zo != 0.0F;
    }

    // Avoid creating a new Quaternionf instance, while avoiding Overwrite usage
    @WrapOperation(
            method = "translateAndRotate",
            at = @At(
                    value = "NEW",
                    target = "()Lorg/joml/Quaternionf;"
            )
    )
    public Quaternionf avoidNewQuaternionInstance(Operation<Quaternionf> original) {
        return TEMP_QUAT;
    }

    @WrapOperation(
            method = "translateAndRotate",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/joml/Quaternionf;rotationZYX(FFF)Lorg/joml/Quaternionf;"
            )
    )
    public Quaternionf skipQuaternionRotation(Quaternionf quaternionf, float angleZ, float angleY, float angleX, Operation<Quaternionf> original) {
        return quaternionf;
    }

    @WrapOperation(
            method = "translateAndRotate",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"
            )
    )
    public void useMatrixHelperForRotation(PoseStack poseStack, Quaternionfc by, Operation<Void> original) {
        MatrixHelper.rotateZYX(poseStack.last(), this.zRot, this.yRot, this.xRot);
    }
}
