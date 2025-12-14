package net.caffeinemc.mods.sodium.api.config.option;

/**
 * Common interface for validators that define a stepped range of integer values.
 */
public interface SteppedValidator extends Validator<Integer> {
    int min();
    int max();
    int step();

    /**
     * Checks if a given value is valid within this range.
     *
     * @param value The value to check.
     * @return True if the value is valid, false otherwise.
     */
    default boolean isValueValid(int value) {
        return value >= min() && value <= max() && (value - min()) % step() == 0;
    }

    /**
     * Gets the spread of the range (max - min).
     *
     * @return The spread of the range.
     */
    default int getSpread() {
        return this.max() - this.min();
    }
}
