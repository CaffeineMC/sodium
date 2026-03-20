package net.caffeinemc.mods.sodium.client.gui;

import com.mojang.blaze3d.platform.Window;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.minecraft.client.Minecraft;

public class FullscreenResolutionRange implements SteppedValidator {
    @Override
    public int min() {
        return 0;
    }

    @Override
    public int max() {
        Window window = Minecraft.getInstance().getWindow();
        if (window != null) {
            var monitor = window.findBestMonitor();
            if (monitor != null) {
                return monitor.getModeCount() - 1;
            }
        }
        return 1;
    }

    @Override
    public int step() {
        return 1;
    }

    @Override
    public boolean isValueValid(int value) {
        return value >= min();
    }
}
