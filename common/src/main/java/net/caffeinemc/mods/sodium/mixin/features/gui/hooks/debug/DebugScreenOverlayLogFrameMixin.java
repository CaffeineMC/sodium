package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.caffeinemc.mods.sodium.client.util.FrameTimeStatistics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Maintain our own frame-time ring buffer (with more samples than vanilla's FPS
// graph storage) by capturing every frame duration that vanilla logs.
@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayLogFrameMixin {
    @Inject(method = "logFrameDuration", at = @At("HEAD"))
    private void sodium$captureFrameDuration(long frameDuration, CallbackInfo ci) {
        FrameTimeStatistics.INSTANCE.logSample(frameDuration);
    }
}
