package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;

/**
 * A record representing a range of integer values with a specified step. When validating a value, it clamps the value to the nearest valid value within the range.
 *
 * @param max The maximum value of the range (inclusive).
 */
public record GUIScaleRange(int max) implements SteppedValidator {
    @Override
    public int min() {
        return 0;
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
