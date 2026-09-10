package net.caffeinemc.mods.sodium.mixin.features.textures.scan;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
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

    @ModifyReturnValue(method = "createAnimationState", at = @At("RETURN"))
    private SpriteContents.AnimationState markUnknownIfUsingCustomAnimationState(SpriteContents.AnimationState animationState) {
        if (animationState != null && !(SpriteContents.AnimationState.class.equals(animationState.getClass()))) {
            this.hasUnknownImageContents = true;
        }

        return animationState;
    }

    @Override
    public boolean sodium$hasUnknownImageContents() {
        return this.hasUnknownImageContents;
    }
}
