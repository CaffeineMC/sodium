package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    @Shadow private @Nullable ClientLevel level;

    @Inject(method = "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void extractVisibleBlockEntities$neoForge(Camera camera,
                                                      float deltaPartialTick,
                                                      LevelRenderState levelRenderState,
                                                      Frustum cullFrustum,
                                                      CallbackInfo ci) {
        ci.cancel();

        SodiumWorldRenderer renderer = ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).sodium$getWorldRenderer();

        renderer.extractBlockEntities(camera, deltaPartialTick, this.level.destructionProgress(), levelRenderState);
    }
}
