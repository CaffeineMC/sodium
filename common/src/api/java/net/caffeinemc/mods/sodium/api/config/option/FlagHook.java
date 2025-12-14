package net.caffeinemc.mods.sodium.api.config.option;

import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * A hook that is triggered when certain option flags are updated.
 */
public interface FlagHook extends Consumer<Collection<Identifier>> {
    /**
     * Gets the identifiers of the flags that trigger this hook.
     *
     * @return A collection of flag identifiers.
     */
    Collection<Identifier> getTriggers();
}
