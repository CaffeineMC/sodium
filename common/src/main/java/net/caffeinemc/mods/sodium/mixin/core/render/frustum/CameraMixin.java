package net.caffeinemc.mods.sodium.mixin.core.render.frustum;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class CameraMixin {
    /**
     * This fixes a bug causing nausea to not affect culling.
     *
     * @return
     */
    @WrapOperation(method = "createCullFrustum", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;perspective(FFFFZ)Lorg/joml/Matrix4f;"))
    private Matrix4f editMatrix(Matrix4f instance, float fovy, float aspect, float zNear, float zFar, boolean zZeroToOne, Operation<Matrix4f> original) {
        var x = original.call(instance, fovy, aspect, zNear, zFar, zZeroToOne);

        var gameRenderer = Minecraft.getInstance().gameRenderer;
        var gameRendererAccessor = ((GameRendererAccessor) Minecraft.getInstance().gameRenderer);
        var player = Minecraft.getInstance().player;

        float worldPartialTicks = gameRenderer.gameRenderState().levelRenderState.worldPartialTicks;
        float screenEffectScale = gameRenderer.gameRenderState().optionsRenderState.screenEffectScale;
        float portalIntensity = gameRenderer.gameRenderState().levelRenderState.playerRenderState.portalEffectIntensity;
        float nauseaIntensity = gameRenderer.gameRenderState().levelRenderState.playerRenderState.nauseaEffectIntensity;
        float spinningEffectIntensity = Math.max(portalIntensity, nauseaIntensity) * screenEffectScale * screenEffectScale;
        if (spinningEffectIntensity > 0.0F) {
            float skew = 5.0F / (spinningEffectIntensity * spinningEffectIntensity + 5.0F) - spinningEffectIntensity * 0.04F;
            skew *= skew;
            Vector3f axis = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            float angle = gameRenderer.gameRenderState().levelRenderState.playerRenderState.spinningEffectAngle * ((float)Math.PI / 180F);
            x.rotate(angle, axis);
            x.scale(1.0F / skew, 1.0F, 1.0F);
            x.rotate(-angle, axis);
        }

        return x;
    }
}
