package net.caffeinemc.mods.sodium.mixin.core.render.world;

import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements FogStorage {
    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Override
    public FogParameters sodium$getFogParameters() {
        return ((FogStorage) this.fogRenderer).sodium$getFogParameters();
    }
}
