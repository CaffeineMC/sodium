package me.jellysquid.mods.sodium.mixin.features.render.compositing;


import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.opengl.GL32C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Framebuffer.class)
public class FramebufferMixin {
    @Shadow
    public int fbo;

    @Shadow
    public int textureWidth;

    @Shadow
    public int textureHeight;

    /**
     * @author JellySquid
     * @reason Use fixed function hardware for framebuffer blits
     */
    @Inject(method = "drawInternal", at = @At("HEAD"), cancellable = true)
    public void blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {
        if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED)) {
            return;
        }

        if (disableBlend) {
            ci.cancel();

            // When blending is not used, we can directly copy the contents of one
            // framebuffer to another using the blitting engine. This can save a lot of time
            // when compared to going through the rasterization pipeline.
            GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, this.fbo);
            GL32C.glBlitFramebuffer(
                    0, 0, width, height,
                    0, 0, width, height,
                    GL32C.GL_COLOR_BUFFER_BIT, GL32C.GL_LINEAR);
            GL32C.glBindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, 0);
        }
    }
}
