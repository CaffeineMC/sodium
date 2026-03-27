package net.caffeinemc.mods.sodium.mixin.workarounds.context_creation;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.compatibility.checks.ModuleScanner;
import net.caffeinemc.mods.sodium.client.compatibility.checks.PostLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.environment.GlContextInfo;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Shadow
    @Final
    static Logger LOGGER;

    @Unique
    private static long wglPrevContext;

    @Inject(method = "initRenderer", at = @At(value = "RETURN"))
    private static void postContextReady(GpuDevice device, CallbackInfo ci) {
        GlContextInfo context = GlContextInfo.create();
        LOGGER.info("OpenGL Vendor: {}", context.vendor());
        LOGGER.info("OpenGL Renderer: {}", context.renderer());
        LOGGER.info("OpenGL Version: {}", context.version());

        // Capture the current WGL context so that we can detect it being replaced later.
        if (Util.getPlatform() == Util.OS.WINDOWS) {
            wglPrevContext = WGL.wglGetCurrentContext(null);
        } else {
            wglPrevContext = MemoryUtil.NULL;
        }
    }

    @Inject(method = "flipFrame", at = @At(value = "RETURN"))
    private static void preSwapBuffers(TracyFrameCapture tracyFrameCapture, CallbackInfo ci) {
        if (Util.getPlatform() != Util.OS.WINDOWS) return;

        if (wglPrevContext == MemoryUtil.NULL) {
            // There is no prior recorded context. Record it.
            GlContextInfo context = GlContextInfo.create();

            NativeWindowHandle handle = () -> GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().handle());

            PostLaunchChecks.onContextInitialized(handle, context);
            ModuleScanner.checkModules(handle);
            wglPrevContext = WGL.wglGetCurrentContext(null);

            return;
        }

        var context = WGL.wglGetCurrentContext(null);

        if (wglPrevContext == context) {
            // The context has not changed.
            return;
        }

        // Something has decided to replace the OpenGL context, which is not a good sign
        LOGGER.warn("The OpenGL context appears to have been suddenly replaced! Something has likely just injected into the game process.");

        // Likely, this indicates a module was injected into the current process. We should check that
        // nothing problematic was just installed.
        ModuleScanner.checkModules(() -> GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().handle()));

        // If we didn't find anything problematic (which would have thrown an exception), then let's just record
        // the new context pointer and carry on.
        wglPrevContext = context;
    }
}
