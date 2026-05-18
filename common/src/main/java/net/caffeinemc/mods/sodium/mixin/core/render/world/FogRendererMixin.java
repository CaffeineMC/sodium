package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class FogRendererMixin implements FogStorage {
    @Unique
    private FogParameters parameters = FogParameters.NONE;

    @Override
    public FogParameters sodium$getFogParameters() {
        return this.parameters;
    }

    @Inject(method = "setupFog", at = @At(value = "RETURN"))
    private void sodium$storeFogParameters(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float f, ClientLevel level, CallbackInfoReturnable<Vector4f> cir, @Local(name = "fog") FogData fog) {
        this.parameters = new FogParameters(fog.color.x, fog.color.y, fog.color.z, fog.color.w, fog.environmentalStart, fog.environmentalEnd, fog.renderDistanceStart, fog.renderDistanceEnd);
    }
}
