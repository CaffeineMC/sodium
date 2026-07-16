package net.caffeinemc.mods.sodium.client.services;

public interface FRAPIRegistrar {
    FRAPIRegistrar INSTANCE = Services.loadConditionalOr(FRAPIRegistrarProvider.class, () -> () -> {}); // Returns a no-op implementation if the platform does not support FRAPI

    static FRAPIRegistrar getInstance() {
        return INSTANCE;
    }

    /**
     * Registers the FRAPI provider. This should only be called once, and should be called during mod initialization.
     */
    void register();
}
