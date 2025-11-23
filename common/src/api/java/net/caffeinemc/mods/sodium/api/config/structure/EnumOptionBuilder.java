package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder interface for defining enum options. Refines builder methods to return this class instead of the base interface and have an {@link Enum} value type.
 *
 * @param <E> The enum type for this option.
 */
public interface EnumOptionBuilder<E extends Enum<E>> extends StatefulOptionBuilder<E> {
    /**
     * Creates a name provider function that maps enum constants to the provided names based on their ordinal values.
     *
     * @param names The array of names corresponding to the enum constants.
     * @param <E>   The enum type.
     * @return A function that provides names for enum constants.
     */
    static <E extends Enum<E>> Function<E, Component> nameProviderFrom(Component... names) {
        return e -> names[e.ordinal()];
    }

    /**
     * Sets the allowed values for this enum option.
     *
     * @param allowedValues The set of allowed enum values.
     * @return This builder instance.
     */
    EnumOptionBuilder<E> setAllowedValues(Set<E> allowedValues);

    /**
     * Sets a provider function to determine the allowed values for this enum option based on the current configuration state.
     *
     * @param provider     The function that provides the set of allowed enum values.
     * @param dependencies The options that this provider depends on.
     * @return This builder instance.
     */
    EnumOptionBuilder<E> setAllowedValuesProvider(Function<ConfigState, Set<E>> provider, ResourceLocation... dependencies);

    /**
     * Sets a provider function to determine the display name for each enum constant.
     *
     * @param provider The function that provides the display name for each enum constant.
     * @return This builder instance.
     */
    EnumOptionBuilder<E> setElementNameProvider(Function<E, Component> provider);

    @Override
    EnumOptionBuilder<E> setName(Component name);

    @Override
    EnumOptionBuilder<E> setStorageHandler(StorageEventHandler storage);

    @Override
    EnumOptionBuilder<E> setTooltip(Component tooltip);

    @Override
    EnumOptionBuilder<E> setTooltip(Function<E, Component> tooltip);

    @Override
    EnumOptionBuilder<E> setImpact(OptionImpact impact);

    @Override
    EnumOptionBuilder<E> setFlags(OptionFlag... flags);

    @Override
    EnumOptionBuilder<E> setDefaultValue(E value);

    @Override
    EnumOptionBuilder<E> setDefaultProvider(Function<ConfigState, E> provider, ResourceLocation... dependencies);

    @Override
    EnumOptionBuilder<E> setEnabled(boolean available);

    @Override
    EnumOptionBuilder<E> setEnabledProvider(Function<ConfigState, Boolean> provider, ResourceLocation... dependencies);

    @Override
    EnumOptionBuilder<E> setBinding(Consumer<E> save, Supplier<E> load);

    @Override
    EnumOptionBuilder<E> setBinding(OptionBinding<E> binding);
}
