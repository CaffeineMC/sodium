package net.caffeinemc.mods.sodium.api.config.option;

import java.util.function.Supplier;

/**
 * Common interface for validators that define a stepped range of integer values.
 */
public interface SteppedValidator extends Validator<Integer> {
    /**
     * Gets the minimum value of the range.
     * @return The minimum value.
     */
    int min();

    /**
     * Gets the maximum value of the range.
     * @return The maximum value.
     */
    int max();

    /**
     * Gets the step increment between valid values in the range.
     *
     * @return The step increment.
     */
    int step();

    /**
     * Checks if a given value is valid within this range.
     *
     * @param value The value to check.
     * @return True if the value is valid, false otherwise.
     */
    default boolean isValueValid(int value) {
        int min = this.min();
        return value >= min && value <= this.max() && (value - min) % this.step() == 0;
    }

    @Override
    default Integer getValidatedValue(Integer value, Supplier<Integer> defaultValueSupplier) {
        if (isValueValid(value)) {
            return value;
        }
        return defaultValueSupplier.get();
    }
}
