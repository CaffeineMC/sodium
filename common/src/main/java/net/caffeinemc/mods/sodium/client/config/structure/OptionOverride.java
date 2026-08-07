package net.caffeinemc.mods.sodium.client.config.structure;

import net.minecraft.resources.ResourceLocation;

public record OptionOverride(ResourceLocation target, String source, Option change, int priority) {
    public OptionOverride(ResourceLocation target, String source, Option change) {
        this(target, source, change, 0);
    }
}
