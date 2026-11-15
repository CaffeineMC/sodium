package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.WritableRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenEntries.class)
public class DebugScreenEntriesMixin {
    @Inject(method = "bootstrap", at = @At("HEAD"))
    private static void sodium$bootstrap(WritableRegistry<DebugScreenEntry> registry, CallbackInfo ci) {
        SodiumClientMod.bootstrapEntries(registry);
    }
}
