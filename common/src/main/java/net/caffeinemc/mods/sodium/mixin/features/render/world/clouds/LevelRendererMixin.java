package net.caffeinemc.mods.sodium.mixin.features.render.world.clouds;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.render.immediate.CloudRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    private @Nullable ClientLevel level;
    @Shadow
    private int ticks;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private CloudRenderer cloudRenderer;

    /**
     * @author jellysquid3
     * @reason Optimize cloud rendering
     */
    @Group(name = "sodium$cloudsOverride", min = 1, max = 1)
    @Inject(
            method = "method_62205",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;F)V"
            ),
            require = 0,
            cancellable = true
    )
    public void renderCloudsFabric(int i, CloudStatus cloudStatus, float f, Vec3 vec3, float g, CallbackInfo ci) {
        ci.cancel();

        this.sodium$renderClouds(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), i);
    }

    @Group(name = "sodium$cloudsOverride", min = 1, max = 1)
    @Dynamic
    @Inject(method = { "lambda$addCloudsPass$6" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CloudRenderer;render(ILnet/minecraft/client/CloudStatus;FLnet/minecraft/world/phys/Vec3;F)V"), cancellable = true, require = 0) // Inject after Forge checks dimension support
    public void renderCloudsNeo(float f, Vec3 vec3, Matrix4f modelView, Matrix4f proj, int i, CloudStatus status, float g, CallbackInfo ci) {
        ci.cancel();

        this.sodium$renderClouds(modelView, proj, i);
    }

    @Unique
    private void sodium$renderClouds(Matrix4f matModelView, Matrix4f matProjection, int color) {
        if (this.cloudRenderer == null) {
            this.cloudRenderer = new CloudRenderer(this.minecraft.getResourceManager());
        }

        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        ClientLevel level = Objects.requireNonNull(this.level);

        var ticks = this.ticks;
        var partialTicks = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        this.cloudRenderer.render(camera, level, matProjection, matModelView, ticks, partialTicks, color);
    }

    @Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("RETURN"))
    private void onReload(ResourceManager manager, CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.reload(manager);
        }
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        // will be re-allocated on next use
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        // will never be re-allocated, as the renderer is shutting down
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }
}
