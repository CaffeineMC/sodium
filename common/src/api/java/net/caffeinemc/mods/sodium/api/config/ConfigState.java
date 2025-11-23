package net.caffeinemc.mods.sodium.api.config;

import net.minecraft.resources.ResourceLocation;

/**
 * Represents the current state of configuration options. This interface is accessed through dynamic value providers throughout the API. Only declared dependencies of a dynamic value provider are allowed to be queried (and doing otherwise will result in a crash).
 */
public interface ConfigState {
    /**
     * Special option ID to be passed as a dependency of a dynamic value provider to indicate that the provider should be re-evaluated when the configuration screen is rebuilt. It is unlikely that you need to use this.
     */
    ResourceLocation UPDATE_ON_REBUILD = ResourceLocation.parse("__meta__:update_on_rebuild");

    /**
     * Reads a boolean option from the configuration state.
     *
     * @param id The ID of the option.
     * @return The current value of the boolean option.
     */
    boolean readBooleanOption(ResourceLocation id);

    /**
     * Reads an integer option from the configuration state.
     *
     * @param id The ID of the option.
     * @return The current value of the integer option.
     */
    int readIntOption(ResourceLocation id);

    /**
     * Reads an enum option from the configuration state.
     *
     * @param id        The ID of the option.
     * @param enumClass The class of the enum.
     * @param <E>       The enum type.
     * @return The current value of the enum option.
     */
    <E extends Enum<E>> E readEnumOption(ResourceLocation id, Class<E> enumClass);
}
