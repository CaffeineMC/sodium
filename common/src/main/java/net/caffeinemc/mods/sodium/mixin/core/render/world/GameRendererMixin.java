package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.util.FogParameters;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.caffeinemc.mods.sodium.client.util.GameRendererStorage;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin implements GameRendererStorage {
    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Unique
    private Matrix4f projection = new Matrix4f();

    @Override
    public FogParameters sodium$getFogParameters() {
        return ((FogStorage) this.fogRenderer).sodium$getFogParameters();
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"))
    private GpuBufferSlice sodium$setProjection(ProjectionMatrixBuffer instance, Matrix4f projectionMatrix, Operation<GpuBufferSlice> original) {
        this.projection.set(projectionMatrix);
        return original.call(instance, projectionMatrix);
    }

    @Override
    public Matrix4fc sodium$getProjectionMatrix() {
        return projection;
    }
}
