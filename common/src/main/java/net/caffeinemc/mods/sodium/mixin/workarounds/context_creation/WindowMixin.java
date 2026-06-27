package net.caffeinemc.mods.sodium.mixin.workarounds.context_creation;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.amd.AmdWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;


@Mixin(Window.class)
public class WindowMixin {
    @Redirect(method = "createGlfwWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"), expect = 0, require = 0)
    private static long wrapGlfwCreateWindow(int width, int height, CharSequence title, long monitor, long share) {
        NvidiaWorkarounds.applyEnvironmentChanges();
        AmdWorkarounds.applyEnvironmentChanges();

        long handles;

        try {
            handles = GLFW.glfwCreateWindow(width, height, title, monitor, share);
        } finally {
            NvidiaWorkarounds.undoEnvironmentChanges();
            AmdWorkarounds.undoEnvironmentChanges();
        }

        return handles;
    }

    @SuppressWarnings("all")
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"), expect = 0, require = 0)
    private long wrapGlfwCreateWindowForge(final IntSupplier width, final IntSupplier height, final Supplier<String> title, final LongSupplier monitor, Operation<Long> op) {
        boolean applyWorkaroundsLate = !PlatformRuntimeInformation.getInstance()
                .platformHasEarlyLoadingScreen();

        if (applyWorkaroundsLate) {
            NvidiaWorkarounds.applyEnvironmentChanges();
            AmdWorkarounds.applyEnvironmentChanges();
        }

        try {
            return op.call(width, height, title, monitor);
        } finally {
            if (applyWorkaroundsLate) {
                NvidiaWorkarounds.undoEnvironmentChanges();
                AmdWorkarounds.undoEnvironmentChanges();
            }
        }
    }
}
