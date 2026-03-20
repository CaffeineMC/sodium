package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder interface for defining stateful options.
 *
 * @param <V> The type of the option's value.
 */
public interface StatefulOptionBuilder<V> extends OptionBuilder {
    @Override
    StatefulOptionBuilder<V> setName(Component name);

    @Override
    OptionBuilder setEnabled(boolean available);

    @Override
    OptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, Identifier... dependencies);

    /**
     * Sets the storage handler for this option.
     *
     * @param storage The storage event handler.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setStorageHandler(StorageEventHandler storage);

    @Override
    StatefulOptionBuilder<V> setTooltip(Component tooltip);

    /**
     * Sets a functional tooltip for this option that changes the text based on the selected value.
     *
     * @param tooltip The function that provides the tooltip based on the option's value.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setTooltip(Function<V, Component> tooltip);

    /**
     * Sets the performance impact level of this option.
     *
     * @param impact The option's performance impact.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setImpact(OptionImpact impact);

    /**
     * Sets flags for this option.
     *
     * @param flags The option flags.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setFlags(OptionFlag... flags);

    /**
     * Sets flags for this option using {@link Identifier} instances.
     *
     * @param flags The flags as identifiers.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setFlags(Identifier... flags);

    /**
     * Sets the default value for this option. The default value is used when the binding returns an invalid value, such as during the first load.
     *
     * @param value The default value.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setDefaultValue(V value);

    /**
     * Sets a provider function to determine the default value for this option based on the current configuration state.
     *
     * @param provider     The function that provides the default value.
     * @param dependencies The options that this provider depends on.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setDefaultProvider(Function<ConfigState, V> provider, Identifier... dependencies);

    /**
     * Sets whether the control for this option should be hidden when the option is disabled. This should only be set to false when the user should know what the state of the option is even when it is disabled, and they cannot interact with it.
     *
     * By default, controls are hidden when disabled.
     *
     * @param hidden True to hide the control when disabled, false to show it.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setControlHiddenWhenDisabled(boolean hidden);

    /**
     * Sets a binding for this option using save and load functions.
     *
     * @param save The function to save the option's value.
     * @param load The function to load the option's value.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setBinding(Consumer<V> save, Supplier<V> load);

    /**
     * Sets a binding for this option using an {@link OptionBinding} instance.
     *
     * @param binding The option binding.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setBinding(OptionBinding<V> binding);

    /**
     * Sets a hook that is triggered after the options' value has been saved if it changed.
     *
     * @param hook The hook to be executed after applying the option.
     * @return The current builder instance.
     */
    StatefulOptionBuilder<V> setApplyHook(Consumer<ConfigState> hook);
}
