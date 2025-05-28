package net.caffeinemc.mods.sodium.mixin.features.render.world.sky;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.util.color.FastCubicSampler;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.AirBasedFogEnvironment;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AirBasedFogEnvironment.class)
public class FogRendererMixin {
    @Redirect(method = "getBaseColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 redirectSampleColor(Vec3 pos, CubicSampler.Vec3Fetcher vec3Fetcher, @Local BiomeManager biomeManager) {
        return FastCubicSampler.sampleColor(pos,
                (i, j, k) -> biomeManager.getNoiseBiomeAtQuart(i, j, k).value().getFogColor(),
                (v) -> v);
    }
}
