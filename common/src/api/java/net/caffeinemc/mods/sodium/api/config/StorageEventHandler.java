package net.caffeinemc.mods.sodium.api.config;

/**
 * Functional interface for handling storage events, such as after a configuration has been saved.
 */
@FunctionalInterface
public interface StorageEventHandler {
    /**
     * Called after options have been saved to their bindings. This is typically used for flushing changes to disk.
     */
    void afterSave();
}
