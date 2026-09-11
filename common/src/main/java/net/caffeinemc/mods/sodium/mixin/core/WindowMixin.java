package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.renderpearl.backend.opengl.GlBackend;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlBackend.class)
public abstract class WindowMixin {
    @Inject(method = "createWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J"))
    public void setAdditionalWindowHints(String title, int width, int height, long flags, CallbackInfoReturnable<Long> cir) {
        if (!PlatformRuntimeInformation.getInstance().platformHasEarlyLoadingScreen()) {
            if (SodiumClientMod.options().performance.useNoErrorGLContext) {
                SDLVideo.SDL_GL_SetAttribute(SDLVideo.SDL_GL_CONTEXT_NO_ERROR, 1);
            }
        }
    }
}
