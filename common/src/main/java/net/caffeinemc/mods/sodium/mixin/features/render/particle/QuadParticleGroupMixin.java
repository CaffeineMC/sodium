package net.caffeinemc.mods.sodium.mixin.features.render.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QuadParticleGroup.class)
public abstract class QuadParticleGroupMixin {

    @Redirect(method = "extractRenderState",at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"))
    public void sodium$checkFogOcclusion(SingleQuadParticle instance, QuadParticleRenderState particleTypeRenderState, Camera camera, float f){
        if(SodiumClientMod.options().performance.useFogOcclusion ? sodium$isParticleFogOccluded(camera.position(),instance.getBoundingBox().getMaxPosition()) : true){
            instance.extract(particleTypeRenderState,camera,f);
        }
    }

    @Unique
    public boolean sodium$isParticleFogOccluded(Vec3 pointA, Vec3 pointB){
        double dx = pointA.x - pointB.x;
        double dz = pointA.z - pointB.z;
        double distance = (dx * dx + dz * dz);

        var shaderFog = ((FogStorage) Minecraft.getInstance().gameRenderer).sodium$getFogParameters();
        double shaderFogDistance = shaderFog.environmentalEnd();

        var renderDistance = Minecraft.getInstance().gameRenderer.getRenderDistance();

        if(!Mth.equal(shaderFog.alpha(),1.0f)){
            return false;
        }

        var fogDist = Math.min(renderDistance,shaderFogDistance);

        return distance < fogDist * fogDist;
    }

}
