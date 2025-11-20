package net.caffeinemc.mods.sodium.client.services;

public interface FRAPIProvider {
    FRAPIProvider INSTANCE = Services.loadOr(FRAPIProvider.class, () -> () -> {}); // Returns a no-op implementation if the platform does not support FRAPI

    static FRAPIProvider getInstance() {
        return INSTANCE;
    }

    /**
     * Registers the FRAPI provider. This should only be called once, and should be called during mod initialization.
     */
    void register();
}
