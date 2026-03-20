package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.client.config.builder.OptionBuilderImpl;
import net.minecraft.resources.Identifier;

public record OptionOverlay(Identifier target, String source, OptionBuilderImpl<?> change) {
}
