package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(DebugScreenEntries.class)
public interface DebugScreenEntriesAccessor {
    @Accessor("ENTRIES_BY_LOCATION")
    static Map<ResourceLocation, DebugScreenEntry> getEntries() {
        return null;
    }
}
