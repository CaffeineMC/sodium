package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.network.chat.Component;

/**
 * Builder interface for defining option groups, which are collections of related options.
 * <p>
 * Option groups only exist to visually group options together in the configuration UI, and a title is not required and won't be displayed unless set.
 */
public interface OptionGroupBuilder {
    /**
     * Sets the name of this option group. This is optional and won't be displayed unless set. We recommend only using this if necessary, as usually option groups without names are enough grouping and option names provide sufficiently detailed labels of the option categories.
     *
     * @param name This option group's display name.
     * @return The current builder instance.
     */
    OptionGroupBuilder setName(Component name);

    /**
     * Adds an option to this option group.
     *
     * @param option The option to add.
     * @return The current builder instance.
     */
    OptionGroupBuilder addOption(OptionBuilder option);
}
