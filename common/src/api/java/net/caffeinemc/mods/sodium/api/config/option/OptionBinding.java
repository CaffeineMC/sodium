package net.caffeinemc.mods.sodium.api.config.option;

/**
 * Interface for binding an option to a storage mechanism.
 *
 * @param <V> The type of the option value.
 */
public interface OptionBinding<V> {
    /**
     * Saves the given value to the storage mechanism.
     *
     * @param value The value to save.
     */
    void save(V value);

    /**
     * Loads the value from the storage mechanism.
     *
     * @return The loaded value.
     */
    V load();
}
