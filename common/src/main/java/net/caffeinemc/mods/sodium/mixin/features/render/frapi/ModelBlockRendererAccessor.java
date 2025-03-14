package net.caffeinemc.mods.sodium.mixin.features.render.frapi;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelBlockRenderer.class)
public interface ModelBlockRendererAccessor {
    @Accessor
    BlockColors getBlockColors();
}
