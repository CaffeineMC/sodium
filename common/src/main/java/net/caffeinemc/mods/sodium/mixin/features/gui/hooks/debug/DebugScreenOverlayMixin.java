package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Inject(method = "extractRenderState", at = @At(value = "RETURN"))
    private void sodium$renderBufferArenaOverlay(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        var debugEntries = Minecraft.getInstance().debugEntries;
        if (debugEntries.isOverlayVisible() && debugEntries.isCurrentlyEnabled(SodiumClientMod.SODIUM_DEBUG_ENTRY_BUFFER_ARENA)) {
            SodiumWorldRenderer.instance().renderBufferDebug(graphics);
        }
    }
}
