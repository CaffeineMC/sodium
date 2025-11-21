package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;
import java.util.function.Function;

public interface ExternalButtonOptionBuilder extends OptionBuilder {
    ExternalButtonOptionBuilder setScreenProvider(Consumer<Screen> currentScreenConsumer);

    @Override
    ExternalButtonOptionBuilder setName(Component name);

    @Override
    ExternalButtonOptionBuilder setTooltip(Component tooltip);

    @Override
    ExternalButtonOptionBuilder setEnabled(boolean available);

    @Override
    ExternalButtonOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, Identifier... dependencies);
}
