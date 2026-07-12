package net.caffeinemc.mods.sodium.client.render.frapi;

import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitterProvider;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;

public class FRAPIEmitterProvider implements PlatformModelEmitterProvider {
    @Override
    public boolean isEnabled() {
        return PlatformRuntimeInformation.getInstance().isModInLoadingList("fabric-renderer-api-v1") 
            || PlatformRuntimeInformation.getInstance().isModInLoadingList("fabric_renderer_api_v1");
    }

    @Override
    public PlatformModelEmitter get() {
        return new FRAPIEmitter();
    }
}
