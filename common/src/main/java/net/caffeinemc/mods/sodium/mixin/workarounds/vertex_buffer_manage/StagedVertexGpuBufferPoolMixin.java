package net.caffeinemc.mods.sodium.mixin.workarounds.vertex_buffer_manage;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.systems.GpuDevice;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(StagedVertexBuffer.GpuBufferPool.class)
public class StagedVertexGpuBufferPoolMixin {
    @Shadow
    @Final
    private List<GpuBuffer> usedThisFrame;

    @Shadow
    @Final
    private List<StagedVertexBuffer.GpuBufferPool.PendingRecycle> pendingRecycle;

    // Never destroy available buffers to avoid unstable glFence sync time make the game create new buffers in every few frames
    @WrapMethod(method = "endFrame")
    private void sodium$endFrame(GpuDevice device, Operation<Void> original) {
        if (!this.usedThisFrame.isEmpty()) {
            GpuFence fence = device.createCommandEncoder().createFence();
            this.pendingRecycle.add(new StagedVertexBuffer.GpuBufferPool.PendingRecycle(List.copyOf(this.usedThisFrame), fence));
            this.usedThisFrame.clear();
        }
    }
}
