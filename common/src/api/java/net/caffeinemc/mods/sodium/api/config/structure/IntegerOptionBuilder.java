package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builder interface for defining integer options. Refines builder methods to return this class instead of the base interface and have an {@link Integer} value type.
 */
public interface IntegerOptionBuilder extends StatefulOptionBuilder<Integer> {
    /**
     * Sets the range for this integer option.
     *
     * @param min  The minimum value (inclusive).
     * @param max  The maximum value (inclusive).
     * @param step The step value for increments.
     * @return The current builder instance.
     */
    IntegerOptionBuilder setRange(int min, int max, int step);

    /**
     * Sets the range for this integer option.
     *
     * @param range The range object defining min, max, and step.
     * @return The current builder instance.
     */
    IntegerOptionBuilder setRange(Range range);

    /**
     * Sets a provider function to determine the range for this integer option based on the current configuration state.
     *
     * @param provider     The function that provides the range.
     * @param dependencies The options that this provider depends on.
     * @return The current builder instance.
     */
    IntegerOptionBuilder setRangeProvider(Function<ConfigState, Range> provider, Identifier... dependencies);

    /**
     * Sets the value formatter for this integer option.
     *
     * @param formatter The formatter to format the integer value of this option.
     * @return The current builder instance.
     */
    IntegerOptionBuilder setValueFormatter(ControlValueFormatter formatter);

    @Override
    IntegerOptionBuilder setName(Component name);

    @Override
    IntegerOptionBuilder setStorageHandler(StorageEventHandler storage);

    @Override
    IntegerOptionBuilder setTooltip(Component tooltip);

    @Override
    IntegerOptionBuilder setTooltip(Function<Integer, Component> tooltip);

    @Override
    IntegerOptionBuilder setImpact(OptionImpact impact);

    @Override
    IntegerOptionBuilder setFlags(OptionFlag... flags);

    @Override
    IntegerOptionBuilder setDefaultValue(Integer value);

    @Override
    IntegerOptionBuilder setDefaultProvider(Function<ConfigState, Integer> provider, Identifier... dependencies);

    @Override
    IntegerOptionBuilder setEnabled(boolean available);

    @Override
    IntegerOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, Identifier... dependencies);

    @Override
    IntegerOptionBuilder setBinding(Consumer<Integer> save, Supplier<Integer> load);

    @Override
    IntegerOptionBuilder setBinding(OptionBinding<Integer> binding);
}