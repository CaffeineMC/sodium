package net.caffeinemc.mods.sodium.mixin.core.gui;

import net.caffeinemc.mods.sodium.client.gpu.device.context.DrawContext;
import net.minecraft.client.gui.components.debug.DebugEntrySimplePerformanceImpactors;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(DebugEntrySimplePerformanceImpactors.class)
public class DebugEntrySimplePerformanceImpactorsMixin {
    @Inject(method = "display", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isChunkRenderingUsingMultiDrawIndirect()Z"), cancellable = true)
    private void sodium$replace(DebugScreenDisplayer displayer, Level serverOrClientLevel, LevelChunk clientChunk, LevelChunk serverChunk, CallbackInfo ci) {
        ci.cancel();
        displayer.addLine(String.format(Locale.ROOT, "Terrain Rendering: %s", DrawContext.create().name()));
    }
}
