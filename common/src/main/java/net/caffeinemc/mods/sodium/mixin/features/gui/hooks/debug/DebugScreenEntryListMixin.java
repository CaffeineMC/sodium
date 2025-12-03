package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(DebugScreenEntryList.class)
public class DebugScreenEntryListMixin {
    @Shadow
    private Map<ResourceLocation, DebugScreenEntryStatus> allStatuses;
    
    @Unique
    private void setFullDebugStatuses() {
        this.allStatuses.put(DebugScreenEntries.CHUNK_RENDER_STATS, DebugScreenEntryStatus.IN_F3);
        this.allStatuses.put(DebugScreenEntries.MEMORY, DebugScreenEntryStatus.IN_F3);
        this.allStatuses.put(DebugScreenEntries.SYSTEM_SPECS, DebugScreenEntryStatus.IN_F3);
    }

    @Unique
    private void setReducedDebugStatuses() {
        this.allStatuses.put(DebugScreenEntries.CHUNK_RENDER_STATS, DebugScreenEntryStatus.IN_F3);
    }

    @Inject(method = "loadDefaultProfile", at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;allStatuses:Ljava/util/Map;"))
    private void injectLoadDefaultProfile(CallbackInfo ci) {
        this.setFullDebugStatuses();
    }

    @Inject(method = "loadProfile", at = @At(value = "FIELD", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/gui/components/debug/DebugScreenEntryList;allStatuses:Ljava/util/Map;"))
    private void injectLoadProfile(DebugScreenProfile debugScreenProfile, CallbackInfo ci) {
        if (debugScreenProfile == DebugScreenProfile.PERFORMANCE && !PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment()) {
            this.setReducedDebugStatuses();
        } else {
            this.setFullDebugStatuses();
        }
    }

    @Inject(method = "rebuildCurrentList", at = @At("HEAD"))
    private void injectSodiumSettings(CallbackInfo ci) {
        ResourceLocation setting = PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment() ? SodiumClientMod.SODIUM_DEBUG_ENTRY_FULL : SodiumClientMod.SODIUM_DEBUG_ENTRY_REDUCED;
        if (!this.allStatuses.containsKey(setting)) {
            this.allStatuses.put(setting, DebugScreenEntryStatus.IN_F3);
        }
    }
}

