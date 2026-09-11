package net.caffeinemc.mods.sodium.mixin.core.render.frustum;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    /**
     * This fixes a bug causing nausea to not affect culling.
     */
    @Inject(method = "createProjectionMatrixForCulling", at = @At("RETURN"), cancellable = true)
    private void editMatrix(CallbackInfoReturnable<Matrix4f> cir) {
        var x = cir.getReturnValue();

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
        cir.setReturnValue(x);

    }
}
