package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    @Inject(method = "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("HEAD"), cancellable = true)
    private void extractVisibleBlockEntities$neoForge(Camera camera, float f, LevelRenderState levelRenderState, Frustum frustum, CallbackInfo ci) {
        ci.cancel();

        SodiumWorldRenderer renderer = ((LevelRendererExtension) this).sodium$getWorldRenderer();

        renderer.extractBlockEntities(camera, f, this.destructionProgress, levelRenderState);
    }
}
