package net.caffeinemc.mods.sodium.api.config.option;

/**
 * A record representing a range of integer values with a specified step. When validating a value, it uses the default value if the value is out of range or does not conform to the step.
 *
 * @param min  The minimum value of the range (inclusive).
 * @param max  The maximum value of the range (inclusive).
 * @param step The step increment between valid values in the range.
 */
public record Range(int min, int max, int step) implements SteppedValidator {
    public Range {
        if (min > max) {
            throw new IllegalArgumentException("Min must be less than or equal to max");
        }
        if (step <= 0) {
            throw new IllegalArgumentException("Step must be greater than 0");
        }
    }
}
