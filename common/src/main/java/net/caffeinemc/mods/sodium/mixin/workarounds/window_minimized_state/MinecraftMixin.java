package net.caffeinemc.mods.sodium.mixin.workarounds.window_minimized_state;

import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** As a workaround for the blitting crash on some Intel GPUs (see
 * {@link Workarounds.Reference#INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED}), vanilla skips framebuffer blitting
 * when the window is minimized. However, the vanilla implementation of {@link Window#isMinimized()} relies on
 * {@link org.lwjgl.glfw.GLFWFramebufferSizeCallback}, which means it only has the information of when the last event
 * polling happened. The problem is that event polling only happens once per frame. The game might think the window is
 * not minimized, if it is minimized after the last event polling.
 * <p>
 * This Mixin replaces the call to {@link Window#isMinimized()} with {@link GLFW#glfwGetFramebufferSize}, which actively
 * queries the actual framebuffer size from the operating system.
 * */
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private final boolean sodium$redirectWindowMinimizedState = Workarounds.isWorkaroundEnabled(Workarounds.Reference.INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED);

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;isMinimized()Z"))
    private boolean redirectWindowMinimized(Window window) {
        if (!sodium$redirectWindowMinimizedState) {
            return window.isMinimized();
        }
        try (var stack = MemoryStack.stackPush()) {
            var width = stack.callocInt(1);
            var height = stack.callocInt(1);
            GLFW.glfwGetFramebufferSize(window.handle(), width, height);
            return width.get(0) == 0 || height.get(0) == 0;
        }
    }
}
