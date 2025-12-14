package net.caffeinemc.mods.sodium.api.config.option;

import java.util.function.Supplier;

/**
 * Common interface for validators that validate values of type V.
 *
 * @param <V> The type of value to be validated.
 */
public interface Validator<V> {
    V getValidatedValue(V value, Supplier<V> defaultValueSupplier);
}
