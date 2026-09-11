package net.caffeinemc.mods.sodium.mixin.workarounds.context_creation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.mojang.renderpearl.backend.opengl.GlBackend;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.amd.AmdWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;


@Mixin(GlBackend.class)
public class WindowMixin {
    @Redirect(
            method = "createWindow",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/sdl/SDLVideo;SDL_CreateWindow(Ljava/lang/CharSequence;IIJ)J"),
            expect = 0,
            require = 0)
    private static long wrapGlfwCreateWindow(CharSequence title, int w, int h, long flags) {
        NvidiaWorkarounds.applyEnvironmentChanges();
        AmdWorkarounds.applyEnvironmentChanges();

        long handles;

        try {
            handles = SDLVideo.SDL_CreateWindow(title, w, h, flags);
        } finally {
            NvidiaWorkarounds.undoEnvironmentChanges();
            AmdWorkarounds.undoEnvironmentChanges();
        }

        return handles;
    }
}
