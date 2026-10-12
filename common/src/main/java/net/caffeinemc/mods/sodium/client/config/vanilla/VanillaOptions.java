package net.caffeinemc.mods.sodium.client.config.vanilla;

import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.minecraft.client.OptionInstance;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.function.Function;

/**
 * A utility class for creating and binding vanilla options to the Sodium configuration system.
 * This class provides methods for creating boolean, enum, and integer options, and binds them to the underlying vanilla option instances.
 */
public class VanillaOptions {
    private final ConfigBuilder builder;
    private final StorageEventHandler storage;

    public VanillaOptions(ConfigBuilder builder, StorageEventHandler storage) {
        this.builder = builder;
        this.storage = storage;
    }

    public BooleanOptionBuilder createBooleanOption(Identifier id, OptionInstance<Boolean> instance) {
        var optionBuilder = this.builder.createBooleanOption(id);

        this.bind(optionBuilder, new VanillaOption<>(instance), Function.identity(), Function.identity());
        return optionBuilder;
    }

    public <E extends Enum<E>> EnumOptionBuilder<E> createEnumOption(
            Identifier id, Class<E> enumClass, OptionInstance<E> instance) {
        var option = new VanillaOption<>(instance);
        var optionBuilder = this.builder.createEnumOption(id, enumClass)
                .setElementNameProvider(option::getElementName);
        if (instance.values() instanceof OptionInstance.Enum<E> values) {
            optionBuilder.setAllowedValues(Set.copyOf(values.values()));
        }

        this.bind(optionBuilder, option, Function.identity(), Function.identity());
        return optionBuilder;
    }

    public IntegerOptionBuilder createIntegerOption(Identifier id, OptionInstance<Integer> instance) {
        return this.createIntegerOption(id, instance, IntProjection.identity());
    }

    public <T> IntegerOptionBuilder createIntegerOption(
            Identifier id, OptionInstance<T> instance, IntProjection<T> projection) {
        var option = new VanillaOption<>(instance);
        var optionBuilder = this.builder.createIntegerOption(id)
                .setRange(option.getRangeFor(projection));

        this.bind(optionBuilder, option, projection::toInt, projection::fromInt);
        return optionBuilder;
    }

    private <T, V> void bind(
            StatefulOptionBuilder<V> optionBuilder, VanillaOption<T> option, Function<T, V> importValue, Function<V, T> exportValue) {
        optionBuilder
                .setStorageHandler(this.storage)
                .setName(option.name())
                .setDefaultValue(importValue.apply(option.defaultValue()))
                .setBinding(value -> option.set(exportValue.apply(value)), () -> importValue.apply(option.get()));

        var tooltipProvider = option.getTooltipProvider();
        if (tooltipProvider != null) {
            optionBuilder.setTooltip(tooltipProvider.compose(exportValue));
        }
    }
}
