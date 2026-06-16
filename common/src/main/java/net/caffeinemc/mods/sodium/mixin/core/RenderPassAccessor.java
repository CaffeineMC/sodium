package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPass.class)
public interface RenderPassAccessor {
    @Accessor
    RenderPassBackend getBackend();
}
