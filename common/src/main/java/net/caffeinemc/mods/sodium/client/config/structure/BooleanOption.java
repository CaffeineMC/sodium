package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.StorageEventHandler;
import net.caffeinemc.mods.sodium.api.config.option.OptionBinding;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.config.value.DependentValue;
import net.caffeinemc.mods.sodium.client.gui.options.control.Control;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class BooleanOption extends StatefulOption<Boolean> {
    public BooleanOption(
            Identifier id,
            Collection<Identifier> dependencies,
            Component name,
            DependentValue<Boolean> enabled,
            StorageEventHandler storage,
            Function<Boolean, Component> tooltipProvider,
            OptionImpact impact,
            Set<Identifier> flags,
            DependentValue<Boolean> defaultValue,
            Boolean controlHiddenWhenDisabled,
            OptionBinding<Boolean> binding,
            Consumer<ConfigState> applyHook
    ) {
        super(id, dependencies, name, enabled, storage, tooltipProvider, impact, flags, defaultValue, controlHiddenWhenDisabled, binding, applyHook);
    }

    @Override
    Control createControl() {
        return new TickBoxControl(this);
    }

    @Override
    Boolean validateValue(Boolean value) {
        return value;
    }
}
