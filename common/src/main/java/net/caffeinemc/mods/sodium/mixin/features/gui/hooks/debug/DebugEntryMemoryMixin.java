package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.client.gui.components.debug.DebugEntryMemory;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.management.ManagementFactory;
import java.util.Locale;

@Mixin(DebugEntryMemory.class)
public class DebugEntryMemoryMixin {
    @Shadow
    @Final
    private static Identifier GROUP;

    @Unique
    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    @Unique
    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }

    @Inject(method = "display", at = @At(value = "RETURN"))
    private void sodium$addOffHeap(DebugScreenDisplayer debugScreenDisplayer, Level level, LevelChunk levelChunk, LevelChunk levelChunk2, CallbackInfo ci) {
        debugScreenDisplayer.addToGroup(GROUP, getNativeMemoryString());
    }
}
