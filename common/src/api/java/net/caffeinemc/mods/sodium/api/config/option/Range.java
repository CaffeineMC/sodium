package net.caffeinemc.mods.sodium.api.config.option;

/**
 * A record representing a range of integer values with a specified step.
 *
 * @param min  The minimum value of the range (inclusive).
 * @param max  The maximum value of the range (inclusive).
 * @param step The step increment between valid values in the range.
 */
public record Range(int min, int max, int step) {
    public Range {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be greater than 0");
        }
    }

    /**
     * Checks if a given value is valid within this range.
     *
     * @param value The value to check.
     * @return True if the value is valid, false otherwise.
     */
    public boolean isValueValid(int value) {
        return value >= this.min && value <= this.max && (value - this.min) % this.step == 0;
    }

    /**
     * Gets the spread of the range (max - min).
     *
     * @return The spread of the range.
     */
    public int getSpread() {
        return this.max - this.min;
    }
}
