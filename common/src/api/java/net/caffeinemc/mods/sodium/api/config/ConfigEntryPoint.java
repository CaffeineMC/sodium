package net.caffeinemc.mods.sodium.api.config;

import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;

/**
 * Entry point interface for registering configuration structure. Typically, mods will only need the "late" registration method unless they need to do something very early in the mod loading process and need their options to be resolved before that point.
 * <p>
 * The {@link ConfigBuilder} instance passed to these methods already knows which mod is registering the configuration, so there is typically no need to specify the mod metadata manually.
 */
public interface ConfigEntryPoint {
    /**
     * Register configuration structure early in the mod loading process.
     * This method is called before most mods have had a chance to initialize.
     * Use this method only if you need your configuration options to be available very early.
     *
     * @param builder The configuration builder to register options with.
     */
    default void registerConfigEarly(ConfigBuilder builder) {
    }

    /**
     * Register configuration structure later in the mod loading process.
     * This method is called after most mods have initialized.
     * This is the preferred method for registering configuration options.
     *
     * @param builder The configuration builder to register options with.
     */
    void registerConfigLate(ConfigBuilder builder);
}
