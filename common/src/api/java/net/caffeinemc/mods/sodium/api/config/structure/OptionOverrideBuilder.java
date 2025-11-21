package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

public interface OptionOverrideBuilder {
    OptionOverrideBuilder setTarget(Identifier target);

    OptionOverrideBuilder setReplacement(OptionBuilder option);
}
