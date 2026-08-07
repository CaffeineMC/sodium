package net.caffeinemc.mods.sodium.client.config.structure;

import net.minecraft.resources.Identifier;

public record OptionOverride(Identifier target, String source, Option change, int priority) {
    public OptionOverride(Identifier target, String source, Option change) {
        this(target, source, change, 0);
    }
}
