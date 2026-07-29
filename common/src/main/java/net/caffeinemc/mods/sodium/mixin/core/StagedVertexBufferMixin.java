package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.systems.GpuDevice;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This backports a fix from 26.3 Snapshot 6 that improves performance on OpenGL.
@Mixin(targets = "net.minecraft.client.renderer.StagedVertexBuffer.GpuBufferPool")
public abstract class StagedVertexBufferMixin {
    @Shadow
    protected abstract void tryRecycleBuffers();

    @Redirect(method = "acquire", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/StagedVertexBuffer$GpuBufferPool;tryRecycleBuffers()V"))
    private void sodium$ignore(@Coerce Object instance) {
        // Do nothing.
    }

    @Inject(method = "endFrame", at = @At("RETURN"))
    private void sodium$onEndFrame(GpuDevice device, CallbackInfo ci) {
        // Do what we just ignored.
        this.tryRecycleBuffers();
    }
}
