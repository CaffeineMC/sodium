package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayAccessor {
    @Accessor("frameTimeLogger")
    LocalSampleLogger sodium$getFrameTimeLogger();
}
