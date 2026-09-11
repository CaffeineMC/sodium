package net.caffeinemc.mods.sodium.mixin.features.render.entity.cull;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
    @WrapMethod(
            method = "shouldRender")
    private boolean preShouldRender(T entity, Frustum culler, double camX, double camY, double camZ, float partialTicks, Operation<Boolean> original) {
        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer == null) {
            return original.call(entity, culler, camX, camY, camZ, partialTicks);
        }

        return renderer.isEntityVisible((EntityRenderer<T, S>) (Object) this, entity, partialTicks)  && original.call(entity, culler, camX, camY, camZ, partialTicks);
    }
}
