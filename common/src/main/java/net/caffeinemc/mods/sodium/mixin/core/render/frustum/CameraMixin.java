package net.caffeinemc.mods.sodium.mixin.core.render.frustum;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * This fixes a bug causing nausea to not affect culling.
     */
    @ModifyReturnValue(method = "createProjectionMatrixForCulling", at = @At("RETURN"))
    private Matrix4f editMatrix(Matrix4f original) {
        final GameRenderer gameRenderer = this.minecraft.gameRenderer;
        final GameRendererAccessor gameRendererAccessor = ((GameRendererAccessor) this.minecraft.gameRenderer);
        final LocalPlayer player = this.minecraft.player;
        final float worldPartialTicks = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        float screenEffectScale = gameRenderer.gameRenderState().optionsRenderState.screenEffectScale;
        float portalIntensity = Mth.lerp(worldPartialTicks, player.oPortalEffectIntensity, player.portalEffectIntensity);
        float nauseaIntensity = player.getEffectBlendFactor(MobEffects.NAUSEA, worldPartialTicks);
        float spinningEffectIntensity = Math.max(portalIntensity, nauseaIntensity) * screenEffectScale * screenEffectScale;
        if (spinningEffectIntensity > 0.0F) {
            float skew = 5.0F / (spinningEffectIntensity * spinningEffectIntensity + 5.0F) - spinningEffectIntensity * 0.04F;
            skew *= skew;
            Vector3f axis = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            float angle = (gameRendererAccessor.getSpinningEffectTime() + worldPartialTicks * gameRendererAccessor.getSpinningEffectSpeed()) * ((float)Math.PI / 180F);
            original.rotate(angle, axis);
            original.scale(1.0F / skew, 1.0F, 1.0F);
            original.rotate(-angle, axis);
        }

        return original;
    }
}
