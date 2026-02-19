package net.caffeinemc.mods.sodium.mixin.core.render.frustum;

import org.joml.FrustumIntersection;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FrustumIntersection.class)
public interface FrustumIntersectionAccessor {
    @Accessor
    Vector4f[] getPlanes();
}