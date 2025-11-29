package net.caffeinemc.mods.sodium.client.config.builder;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.value.ConstantValue;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.config.value.DynamicValue;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.function.Function;

public abstract class OptionBuilderImpl<O extends Option> implements OptionBuilder {
    final ResourceLocation id;

    private O baseOption;

    private Component name;
    private DependentValue<Boolean> enabled;

    OptionBuilderImpl(ResourceLocation id) {
        this.id = id;
    }

    abstract O build();

    abstract Class<O> getOptionClass();

    public O buildWithBaseOption(Option baseOption) {
        Validate.isTrue(this.getOptionClass().isInstance(baseOption), "Base option must be of type %s", this.getOptionClass().getSimpleName());

        @SuppressWarnings("unchecked")
        O castedBaseOption = (O) baseOption;
        this.baseOption = castedBaseOption;

        return this.build();
    }

    void validateData() {
        Validate.notNull(this.getName(), "Name must be set");
    }

    void prepareBuild() {
        this.validateData();

        if (this.getEnabled() == null) {
            this.enabled = new ConstantValue<>(true);
        }
    }

    Collection<ResourceLocation> getDependencies() {
        var dependencies = new ObjectLinkedOpenHashSet<ResourceLocation>();
        dependencies.addAll(this.getEnabled().getDependencies());
        return dependencies;
    }

    public <V> V getFirstNotNull(V overlayValue,  Function<O, V> extractor) {
        if (overlayValue != null) {
            return overlayValue;
        } else if (this.baseOption != null) {
            return extractor.apply(this.baseOption);
        } else {
            return null;
        }
    }

    Component getName() {
        return getFirstNotNull(this.name, Option::getName);
    }

    DependentValue<Boolean> getEnabled() {
        return getFirstNotNull(this.enabled, Option::getEnabled);
    }

    @Override
    public OptionBuilder setName(Component name) {
        Validate.notNull(name, "Argument must not be null");

        this.name = name;
        return this;
    }

    @Override
    public OptionBuilder setEnabled(boolean available) {
        this.enabled = new ConstantValue<>(available);
        return this;
    }

    @Override
    public OptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, ResourceLocation... dependencies) {
        Validate.notNull(provider, "Argument must not be null");

        this.enabled = new DynamicValue<>(provider, dependencies);
        return this;
    }
}
