package net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_upload;

import java.nio.ByteBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.vertex.VertexFormat;

/**
 * Fixes an issue where buffer uploads can be slow on some systems due to Vanilla using {@code glBufferSubData} in 1.21.5 (MC-295893).
 */
@Mixin(VertexFormat.class)
public class VertexFormatMixin {

    @ModifyReceiver(method = { "uploadImmediateVertexBuffer", "uploadImmediateIndexBuffer" }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/GpuBuffer;size()I"), require = 2)
    private GpuBuffer replaceBufferData(GpuBuffer gpuBuffer, ByteBuffer data, @Local CommandEncoder commandEncoder, @Cancellable CallbackInfoReturnable<GpuBuffer> cir) {
        //Setting the initialized field to false forces the glBufferSubData path to be skipped in GlCommandEncoder
        ((GlBufferAccessor) gpuBuffer).setInitialized(false);
        //Update the size of the GpuBuffer
        gpuBuffer.size = data.remaining();

        //Write the data and cancel the method
        commandEncoder.writeToBuffer(gpuBuffer, data, 0);
        cir.setReturnValue(gpuBuffer);

        return gpuBuffer;
    }
}
