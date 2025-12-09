package net.caffeinemc.mods.sodium.mixin.features.textures.scan;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtension {
    @Unique
    private boolean hasUnknownImageContents;

    @WrapOperation(method = "createAnimationState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;createAnimationState(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;I)Lnet/minecraft/client/renderer/texture/SpriteContents$AnimationState;"))
    private SpriteContents.AnimationState hookTickerInstantiation(SpriteContents instance, GpuBufferSlice gpuBufferSlice, int i, Operation<SpriteContents.AnimationState> original) {
        var ticker = original.call(instance, gpuBufferSlice, i);

        if (ticker != null && !(SpriteContents.AnimationState.class.equals(ticker.getClass()))) {
            this.hasUnknownImageContents = true;
        }

        return ticker;
    }

    @Override
    public boolean sodium$hasUnknownImageContents() {
        return this.hasUnknownImageContents;
    }
}
