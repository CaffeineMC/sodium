package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.ResourceLocation;

/**
 * Builder interface for defining option overrides, which replace an existing option with a new one.
 */
public interface OptionOverrideBuilder {
    /**
     * Sets the target option to be overridden.
     *
     * @param target The ID of the target option.
     * @return The current builder instance.
     */
    OptionOverrideBuilder setTarget(ResourceLocation target);

    /**
     * Sets the replacement option.
     *
     * @param option The option builder for the replacement option.
     * @return The current builder instance.
     */
    OptionOverrideBuilder setReplacement(OptionBuilder option);
}
