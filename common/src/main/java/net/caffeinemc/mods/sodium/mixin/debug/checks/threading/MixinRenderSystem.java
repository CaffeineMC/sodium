package net.caffeinemc.mods.sodium.mixin.debug.checks.threading;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {
    /**
     * @author JellySquid
     * @reason Disallow the use of RenderSystem.recordRenderCall entirely
     */
    @Overwrite(remap = false)
    public static void recordRenderCall(RenderCall call) {
        throw new UnsupportedOperationException("Usage of RenderSystem#recordRenderCall is likely a bug, " +
                "which is handled as an error when Sodium is enabled in debug mode");
    }
}
