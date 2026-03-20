package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.ControlValueFormatter;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.option.SteppedValidator;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class IntegerOption extends StatefulOption<Integer> {
    private final DependentValue<? extends SteppedValidator> validator;
    private final ControlValueFormatter valueFormatter;

    public IntegerOption(
            Identifier id,
            Collection<Identifier> dependencies,
            Component name,
            DependentValue<Boolean> enabled,
            StorageEventHandler storage,
            Function<Integer, Component> tooltipProvider,
            OptionImpact impact,
            Set<Identifier> flags,
            DependentValue<Integer> defaultValue,
            Boolean controlHiddenWhenDisabled,
            OptionBinding<Integer> binding,
            Consumer<ConfigState> applyHook,
            DependentValue<? extends SteppedValidator> validator,
            ControlValueFormatter valueFormatter
    ) {
        super(id, dependencies, name, enabled, storage, tooltipProvider, impact, flags, defaultValue, controlHiddenWhenDisabled, binding, applyHook);
        this.validator = validator;
        this.valueFormatter = valueFormatter;
    }

    @Override
    void visitDependentValues(Consumer<DependentValue<?>> visitor) {
        super.visitDependentValues(visitor);
        visitor.accept(this.validator);
    }

    @Override
    Integer validateValue(Integer value) {
        if (this.validator != null) {
            return this.validator.get(this.state).getValidatedValue(value, () -> this.defaultValue.get(this.state));
        } else {
            return value;
        }
    }

    @Override
    Control createControl() {
        return new SliderControl(this);
    }

    public SteppedValidator getSteppedValidator() {
        return this.validator.get(this.state);
    }

    public Component formatValue(int value) {
        return this.valueFormatter.format(value);
    }

    public DependentValue<? extends SteppedValidator> getValidatorProvider() {
        return this.validator;
    }

    public ControlValueFormatter getValueFormatter() {
        return this.valueFormatter;
    }
}

