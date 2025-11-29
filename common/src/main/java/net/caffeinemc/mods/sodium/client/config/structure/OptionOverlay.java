package net.caffeinemc.mods.sodium.client.config.structure;

import net.caffeinemc.mods.sodium.client.config.builder.OptionBuilderImpl;
import net.minecraft.resources.ResourceLocation;

public record OptionOverlay(ResourceLocation target, String source, OptionBuilderImpl<?> change) {
}
