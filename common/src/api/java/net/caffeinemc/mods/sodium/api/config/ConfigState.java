package net.caffeinemc.mods.sodium.api.config;

import net.minecraft.resources.ResourceLocation;

public interface ConfigState {
    ResourceLocation UPDATE_ON_REBUILD = ResourceLocation.parse("__meta__:update_on_rebuild");
    
    boolean readBooleanOption(ResourceLocation id);

    int readIntOption(ResourceLocation id);

    <E extends Enum<E>> E readEnumOption(ResourceLocation id, Class<E> enumClass);
}
