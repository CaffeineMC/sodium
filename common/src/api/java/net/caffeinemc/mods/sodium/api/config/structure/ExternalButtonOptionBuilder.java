package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builder interface for defining external button options.
 */
public interface ExternalButtonOptionBuilder extends OptionBuilder {
    /** Sets the screen consumer for the external button option.
     *
     * @param currentScreenConsumer A consumer that accepts the current screen and opens the external configuration screen.
     * @return The current builder instance.
     */
    ExternalButtonOptionBuilder setScreenConsumer(Consumer<Screen> currentScreenConsumer);

    @Override
    ExternalButtonOptionBuilder setName(Component name);

    @Override
    ExternalButtonOptionBuilder setTooltip(Component tooltip);

    @Override
    ExternalButtonOptionBuilder setEnabled(boolean available);

    @Override
    ExternalButtonOptionBuilder setEnabledProvider(Function<ConfigState, Boolean> provider, ResourceLocation... dependencies);
}
