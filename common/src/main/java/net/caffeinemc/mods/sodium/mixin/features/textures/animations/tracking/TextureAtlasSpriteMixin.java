package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class TextureAtlasSpriteMixin {
    @Inject(method = "wrap", at = @At("HEAD"))
    private void markSpriteAsActive(CallbackInfoReturnable<VertexConsumer> cir) {
        SpriteUtil.INSTANCE.markSpriteActive((TextureAtlasSprite) (Object) this);
    }
}
