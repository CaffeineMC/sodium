package net.caffeinemc.mods.sodium.mixin.features.gui;

import org.spongepowered.asm.mixin.gen.Accessor;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.Options.class)
public interface OptionsAccessor {
    @Accessor("initialExclusiveFullscreen")
    boolean sodium$initialExclusiveFullscreen();
}
