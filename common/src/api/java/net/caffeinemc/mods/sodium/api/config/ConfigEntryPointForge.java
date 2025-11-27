package net.caffeinemc.mods.sodium.api.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a configuration entry point for Sodium on the NeoForge platform.
 * This annotation should be placed on classes that implement the configuration
 * entry point interface to associate them with a specific mod id.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigEntryPointForge {
    /**
     * The mod id to associate this config entrypoint's "owner" with.
     *
     * @return the mod id
     */
    String value();
}
