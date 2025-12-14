package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.function.Consumer;

public class FlagHookImpl implements FlagHook {
    private final Consumer<Collection<Identifier>> hook;
    private final Collection<Identifier> triggers;

    public FlagHookImpl(Consumer<Collection<Identifier>> hook, Collection<Identifier> triggers) {
        this.hook = hook;
        this.triggers = triggers;
    }

    @Override
    public Collection<Identifier> getTriggers() {
        return this.triggers;
    }

    @Override
    public void accept(Collection<Identifier> flags) {
        this.hook.accept(flags);
    }
}
