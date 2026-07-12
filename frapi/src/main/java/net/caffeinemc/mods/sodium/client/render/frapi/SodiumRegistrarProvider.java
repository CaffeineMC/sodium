package net.caffeinemc.mods.sodium.client.render.frapi;

import net.caffeinemc.mods.sodium.client.services.FRAPIRegistrar;
import net.caffeinemc.mods.sodium.client.services.FRAPIRegistrarProvider;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;

public class SodiumRegistrarProvider implements FRAPIRegistrarProvider {
    @Override
    public boolean isEnabled() {
        return PlatformRuntimeInformation.getInstance().isModInLoadingList("fabric-renderer-api-v1") 
            || PlatformRuntimeInformation.getInstance().isModInLoadingList("fabric_renderer_api_v1");
    }

    @Override
    public FRAPIRegistrar get() {
        return new SodiumRegistrar();
    }
}
