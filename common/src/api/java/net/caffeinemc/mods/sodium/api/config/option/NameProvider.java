package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.network.chat.Component;

/**
 * Base interface extended by enums whose members can provide display names.
 */
public interface NameProvider {
    /**
     * Gets the display name of this item.
     *
     * @return the display name
     */
    Component getName();
}
