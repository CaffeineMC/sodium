package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.shaders.GpuDebugOptions;
import com.mojang.blaze3d.shaders.ShaderSource;
import org.lwjgl.opengl.GL33;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(GlDevice.class)
public class GlDeviceMixin {
    @Shadow
    protected static boolean USE_GL_ARB_buffer_storage;

    // Disable ARB_buffer_storage on NVIDIA and Intel Gen7 devices, to avoid some strange stall behavior.
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/opengl/BufferStorage;create(Lorg/lwjgl/opengl/GLCapabilities;Ljava/util/Set;)Lcom/mojang/blaze3d/opengl/BufferStorage;"))
    private void sodium$disableBufferStorage(long windowHandle, ShaderSource defaultShaderSource, GpuDebugOptions debugOptions, CallbackInfo ci) {
        String vendor = GlStateManager._getString(GL33.GL_VENDOR);
        String renderer = GlStateManager._getString(GL33.GL_RENDERER);

        boolean isNvidia = renderer.toLowerCase(Locale.ROOT).contains("nvidia");
        boolean couldBeIntelGen7;
        if (!vendor.contains("intel")) {
            couldBeIntelGen7 = false;
        } else if (renderer.contains("2500")) {
            couldBeIntelGen7 = true;
        } else if (renderer.contains("4000")) {
            couldBeIntelGen7 = true;
        } else {
            couldBeIntelGen7 = renderer.contains("hd graphics (byt)") || renderer.endsWith("hd graphics");
        }

        USE_GL_ARB_buffer_storage = !(isNvidia || couldBeIntelGen7);
    }
}
