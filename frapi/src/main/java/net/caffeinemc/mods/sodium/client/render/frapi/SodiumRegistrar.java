package net.caffeinemc.mods.sodium.client.render.frapi;

import net.caffeinemc.mods.sodium.client.services.FRAPIRegistrar;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;

public class SodiumRegistrar implements FRAPIRegistrar {
    public void register() {
        Renderer.register(SodiumRenderer.INSTANCE);
    }
}
