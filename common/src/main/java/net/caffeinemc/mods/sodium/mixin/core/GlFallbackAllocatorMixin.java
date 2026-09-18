package net.caffeinemc.mods.sodium.mixin.core;

import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This fixes a bug in Minecraft 26.3 where the upload functions for the fallback allocator use unsyncronized access, when it does need a sync.
// This solution attempts to port over how the persistent allocator handles it.
@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlTransientMemory$Fallback")
public class GlFallbackAllocatorMixin {
    @Unique
    private static final int IN_FLIGHT = 2;

    @Unique
    private final Runnable[] sodium$rotations = new Runnable[IN_FLIGHT - 1];

    @Unique
    private int sodium$currentRotation;

    @Redirect(method = "rotate", at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V"))
    private void sodium$waitForFlightBeforeReusing(Runnable r) {
        Runnable previousRotation = this.sodium$rotations[this.sodium$currentRotation];
        this.sodium$rotations[this.sodium$currentRotation] = r;
        this.sodium$currentRotation = (this.sodium$currentRotation + 1) % this.sodium$rotations.length;

        if (previousRotation != null) {
            previousRotation.run();
        }
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void sodium$close(CallbackInfo ci) {
        GL33C.glFinish();

        ci.cancel();

        for (int i = 0; i < this.sodium$rotations.length; i++) {
            Runnable r = this.sodium$rotations[i];
            if (r != null) {
                r.run();
                this.sodium$rotations[i] = null;
            }
        }
    }
}
