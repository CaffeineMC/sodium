package net.caffeinemc.mods.sodium.mixin.workarounds.context_creation;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.opengl.GlSurface;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.compatibility.checks.ModuleScanner;
import net.caffeinemc.mods.sodium.client.compatibility.checks.PostLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.environment.GlContextInfo;
import net.caffeinemc.mods.sodium.client.platform.NativeWindowHandle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlSurface.class)
public class RenderSystemMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-GlSurface");

    @Unique
    private static long wglPrevContext;

    @Unique
    private static boolean hasDonePostLaunchChecks = false;

    @Unique
    private static void doChecksOnce() {
        if (hasDonePostLaunchChecks) {
            return;
        }

        // note the position of this assignment is here to prevent checkModules from running twice when the game renders the last frame before shutting down after checkModules throws an exception and aborts control flow
        hasDonePostLaunchChecks = true;

        LOGGER.info(String.valueOf(Thread.currentThread()));

        GlContextInfo context = GlContextInfo.create();
        LOGGER.info("OpenGL Vendor: {}", context.vendor());
        LOGGER.info("OpenGL Renderer: {}", context.renderer());
        LOGGER.info("OpenGL Version: {}", context.version());

        NativeWindowHandle handle = () -> GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().handle());

        PostLaunchChecks.onContextInitialized(handle, context);
        ModuleScanner.checkModules(handle);
    }

    @Inject(method = "present", at = @At(value = "RETURN"))
    private static void preSwapBuffers(CallbackInfo ci) {
        doChecksOnce();

        // wglGetCurrentContext is only applicable on Windows
        if (Util.getPlatform() != Util.OS.WINDOWS) return;

        if (wglPrevContext == MemoryUtil.NULL) {
            // There is no prior recorded context. Record it.
            wglPrevContext = WGL.wglGetCurrentContext(null);

            return;
        }

        var currentWglContext = WGL.wglGetCurrentContext(null);

        if (wglPrevContext == currentWglContext) {
            // The context has not changed.
            return;
        }

        // record the current context for the next check,
        // we do this here to prevent a duplicate call to checkModules when the game renders on last frame before shutting down after checkModules throws an exception
        wglPrevContext = currentWglContext;

        // Something has decided to replace the OpenGL context, which is not a good sign
        LOGGER.warn("The OpenGL context appears to have been suddenly replaced! Something has likely just injected into the game process.");

        // Likely, this indicates a module was injected into the current process. We should check that
        // nothing problematic was just installed.
        ModuleScanner.checkModules(() -> GLFWNativeWin32.glfwGetWin32Window(Minecraft.getInstance().getWindow().handle()));
    }
}
