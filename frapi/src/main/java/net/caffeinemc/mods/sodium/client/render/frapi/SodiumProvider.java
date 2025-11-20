package net.caffeinemc.mods.sodium.client.render.frapi;

import net.caffeinemc.mods.sodium.client.services.FRAPIProvider;
import net.fabricmc.fabric.api.renderer.v1.Renderer;

public class SodiumProvider implements FRAPIProvider {
    @Override
    public void register() {
        Renderer.register(SodiumRenderer.INSTANCE);
    }
}
