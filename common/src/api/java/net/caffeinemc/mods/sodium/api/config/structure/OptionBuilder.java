package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

/**
 * The base interface for option builders.
 */
public interface OptionBuilder {
    /**
     * Sets the name of the option.
     *
     * @param name The display name of the option.
     * @return The current builder instance.
     */
    OptionBuilder setName(Component name);

    /**
     * Sets the tooltip of the option.
     *
     * @param tooltip The tooltip component.
     * @return The current builder instance.
     */
    OptionBuilder setTooltip(Component tooltip);

    /**
     * Sets whether the option is enabled.
     *
     * @param available True if the option is enabled, false otherwise.
     * @return The current builder instance.
     */
    OptionBuilder setEnabled(boolean available);

    /**
     * Sets a provider function to determine whether the option is enabled based on the current configuration state.
     *
     * @param provider     The function that provides the enabled state.
     * @param dependencies The options that this provider depends on.
     * @return The current builder instance.
     */
    OptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, Identifier... dependencies);
}
