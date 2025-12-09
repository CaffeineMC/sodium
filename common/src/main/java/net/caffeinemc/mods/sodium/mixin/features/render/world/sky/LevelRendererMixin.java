package net.caffeinemc.mods.sodium.mixin.features.render.world.sky;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow
    protected abstract boolean doesMobEffectBlockSky(Camera camera);

    /**
     * <p>Prevents the sky layer from rendering when the fog distance is reduced
     * from the default. This helps prevent situations where the sky can be seen
     * through chunks culled by fog occlusion. This also fixes the vanilla issue
     * <a href="https://bugs.mojang.com/browse/MC-152504">MC-152504</a> since it
     * is also caused by being able to see the sky through invisible chunks.</p>
     *
     * <p>However, this fix comes with some caveats. When underwater, it becomes
     * impossible to see the sun, stars, and moon since the sky is not rendered.
     * While this does not exactly match the vanilla game, it is consistent with
     * what Bedrock Edition does, so it can be considered vanilla-style. This is
     * also more "correct" in the sense that underwater fog is applied to chunks
     * outside of water, so the fog should also be covering the sun and sky.</p>
     *
     * <p>When updating Sodium to new releases of the game, please check for new
     * ways the fog can be reduced in {@link FogRenderer#setupFog(Camera, FogRenderer.FogMode, org.joml.Vector4f, float, boolean, float)} ()}.</p>
     *
     * @return
     */
    @WrapOperation(method = "addSkyPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doesMobEffectBlockSky(Lnet/minecraft/client/Camera;)Z"))
    private static boolean preRenderSky(LevelRenderer instance, Camera camera, Operation<Boolean> original) {
        // Cancels sky rendering when the camera is submersed underwater.
        // This prevents the sky from being visible through chunks culled by Sodium's fog occlusion.
        // Fixes https://bugs.mojang.com/browse/MC-152504.
        // Credit to bytzo for noticing the change in 1.18.2.
        if (Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) {
            return true;
        }
        return original.call(instance, camera);
    }
}
