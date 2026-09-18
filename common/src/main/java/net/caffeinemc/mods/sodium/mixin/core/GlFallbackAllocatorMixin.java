package net.caffeinemc.mods.sodium.mixin.core;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lwjgl.opengl.GL33C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// This fixes a bug in Minecraft 26.3 where the upload functions for the fallback allocator use unsyncronized access, when it does need a sync.
@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlTransientMemory$Fallback")
public class GlFallbackAllocatorMixin {
    @WrapOperation(method = { "uploadGpu", "multiUploadGpu" }, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;nglMapBufferRange(IJJI)J", remap = false))
    private long sodium$synchronizeMapping(int target, long offset, long length, int access, Operation<Long> original) {
        return original.call(target, offset, length, access & ~GL33C.GL_MAP_UNSYNCHRONIZED_BIT);
    }
}