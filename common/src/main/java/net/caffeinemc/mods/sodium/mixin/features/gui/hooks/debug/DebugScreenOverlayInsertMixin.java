package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.util.FrameTimeStatistics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Locale;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayInsertMixin {
    @Inject(
            method = "extractRenderState",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;Z)V", ordinal = 0)
    )
    private void sodium$insertFpsPercentiles(GuiGraphicsExtractor graphics, CallbackInfo ci, @Local(ordinal = 0) List<String> leftLines) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.debugEntries.isCurrentlyEnabled(SodiumClientMod.SODIUM_FPS_PERCENTILES)) {
            return;
        }
        var results = FrameTimeStatistics.INSTANCE.get();
        if (results == null || results.isEmpty()) {
            return;
        }

        // splice the percentile fps display into the debug lines to make sure it's right under the fps string.
        // without this, it may be put somewhere else on the screen.
        int insertAt = 0;
        for (int i = 0; i < leftLines.size(); i++) {
            String line = leftLines.get(i);
            if (line != null && line.contains(" fps T:")) {
                insertAt = i + 1;
                break;
            }
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
                    .append(sodium$nanosToFps(ns))
                    .append(ChatFormatting.GRAY).append(" (").append(sodium$nanosToMs(ns)).append("ms)");
        }

        leftLines.add(insertAt, sb.toString());
    }

    @Unique
    private static long sodium$nanosToFps(long ns) {
        return ns > 0L ? Math.round(1.0e9 / ns) : 0L;
    }

    @Unique
    private static String sodium$nanosToMs(long ns) {
        double ms = ns / 1.0e6;
        if (ms >= 100.0) {
            return String.format(Locale.ROOT, "%.0f", ms);
        }
        if (ms >= 10.0) {
            return String.format(Locale.ROOT, "%.1f", ms);
        }
        if (ms >= 1.0) {
            return String.format(Locale.ROOT, "%.2f", ms);
        }
        return String.format(Locale.ROOT, "%.3f", ms);
    }
}
