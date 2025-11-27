package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Builder interface for defining external configuration pages.
 */
public interface ExternalPageBuilder extends PageBuilder {
    /**
     * Sets the name of the external configuration page.
     *
     * @param name The name component.
     * @return The current builder instance.
     */
    ExternalPageBuilder setName(Component name);

    /**
     * Sets the screen provider for the external configuration page.
     *
     * @param currentScreenConsumer A consumer that accepts the current screen and opens the external configuration screen.
     * @return The current builder instance.
     */
    ExternalPageBuilder setScreenProvider(Consumer<Screen> currentScreenConsumer);
}
