package net.caffeinemc.mods.sodium.mixin.workarounds.window_minimized_state;

import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private final boolean sodium$redirectWindowMinimizedState = Workarounds.isWorkaroundEnabled(Workarounds.Reference.INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED);

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;isMinimized()Z"))
    private boolean redirectWindowMinimized(Window window) {
        if (!sodium$redirectWindowMinimizedState) {
            return window.isMinimized();
        }
        return GLFW.glfwGetWindowAttrib(window.handle(), GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
    }
}
