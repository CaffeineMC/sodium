package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.opengl.GlBackend;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlBackend.class)
public abstract class WindowMixin {
    @Inject(method = "setWindowHints", at = @At("RETURN"))
    public void setAdditionalWindowHints(CallbackInfo ci) {
        if (!PlatformRuntimeInformation.getInstance().platformHasEarlyLoadingScreen()) {
            if (SodiumClientMod.options().performance.useNoErrorGLContext) {
                if (!Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED)) {
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
                }
            }
        }
    }
}
