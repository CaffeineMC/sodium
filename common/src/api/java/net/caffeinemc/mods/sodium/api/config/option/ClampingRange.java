package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.util.Mth;

import java.util.function.Supplier;

/**
 * A record representing a range of integer values with a specified step. When validating a value, it clamps the value to the nearest valid value within the range.
 *
 * @param min  The minimum value of the range (inclusive).
 * @param max  The maximum value of the range (inclusive).
 * @param step The step increment between valid values in the range.
 */
public record ClampingRange(int min, int max, int step) implements SteppedValidator {
    public ClampingRange {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be greater than 0");
        }
    }

    @Override
    public Integer getValidatedValue(Integer value, Supplier<Integer> defaultValueSupplier) {
        if (value < this.min) {
            return this.min;
        } else if (value > this.max) {
            return this.max;
        } else {
            int adjustedValue = this.min + ((value - this.min + this.step / 2) / this.step) * this.step;
            return Mth.clamp(adjustedValue, this.min, this.max);
        }
    }
}
