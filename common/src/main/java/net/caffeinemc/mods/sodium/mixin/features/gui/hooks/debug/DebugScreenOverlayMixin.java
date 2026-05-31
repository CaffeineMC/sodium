package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.util.FrameTimeStatistics;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayMixin {
    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<String> redirectRightTextEarly(Object[] elements) {
        ArrayList<String> strings = Lists.newArrayList((String[]) elements);
        strings.add("");
        strings.add("%sSodium Renderer (%s)".formatted(getVersionColor(), SodiumClientMod.getVersion()));

        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer != null) {
            strings.addAll(renderer.getDebugStrings());
        }

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());

                break;
            }
        }

        return strings;
    }

    @Unique
    private static ChatFormatting getVersionColor() {
        String version = SodiumClientMod.getVersion();
        ChatFormatting color;

        if (version.contains("-local")) {
            color = ChatFormatting.RED;
        } else if (version.contains("-snapshot")) {
            color = ChatFormatting.LIGHT_PURPLE;
        } else {
            color = ChatFormatting.GREEN;
        }

        return color;
    }

    @Unique
    private static String getNativeMemoryString() {
        return "Off-Heap: +" + MathUtil.toMib(getNativeMemoryUsage()) + "MB";
    }

    @Unique
    private static long getNativeMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() + NativeBuffer.getTotalAllocated();
    }

    /**
     * Maintain our own frame-time ring buffer (with more samples than vanilla's FPS
     * graph storage) by capturing every frame duration that vanilla logs.
     */
    @Inject(method = "logFrameDuration", at = @At("HEAD"))
    private void sodium$captureFrameDuration(long frameDuration, CallbackInfo ci) {
        FrameTimeStatistics.INSTANCE.logSample(frameDuration);
    }

    @ModifyReturnValue(method = "getGameInformation", at = @At("RETURN"))
    private List<String> sodium$insertFpsPercentiles(List<String> lines) {
        var results = FrameTimeStatistics.INSTANCE.get();
        if (results == null || results.isEmpty()) {
            return lines;
        }

        var sb = new StringBuilder();
        for (var entry : results.reference2LongEntrySet()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            long ns = entry.getLongValue();
            sb.append(ChatFormatting.GRAY)
                    .append(entry.getKey().name()).append('=')
                    .append(ChatFormatting.RESET)
                    .append(sodium$nanosToFps(ns));
        }
        sb.append(ChatFormatting.GRAY).append(" fps");

        // put it right after the fps line, which vanilla always emits as the second entry
        lines.add(2, sb.toString());

        return lines;
    }

    @Unique
    private static long sodium$nanosToFps(long ns) {
        return ns > 0L ? Math.round(1.0e9 / ns) : 0L;
    }
}
