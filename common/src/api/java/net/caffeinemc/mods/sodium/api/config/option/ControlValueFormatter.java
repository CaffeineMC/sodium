package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.network.chat.Component;

/**
 * A formatter for control values, converting integer values into display components.
 */
@FunctionalInterface
public interface ControlValueFormatter {
    /**
     * Formats the given integer value into a display component.
     *
     * @param value the integer value to format
     * @return the formatted value
     */
    Component format(int value);
}
