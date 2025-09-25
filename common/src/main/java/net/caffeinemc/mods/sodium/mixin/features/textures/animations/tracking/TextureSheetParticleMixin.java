package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SingleQuadParticle.class)
public abstract class TextureSheetParticleMixin {
    @Shadow
    protected TextureAtlasSprite sprite;

    @Unique
    private boolean shouldTickSprite;

    @Inject(method = "setSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("RETURN"))
    private void afterSetSprite(TextureAtlasSprite sprite, CallbackInfo ci) {
        this.shouldTickSprite = sprite != null && SpriteUtil.INSTANCE.hasAnimation(sprite);
    }

    @Inject(method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V", at = @At("HEAD"))
    private void sodium$tickSprite(QuadParticleRenderState quadParticleRenderState, Quaternionf quaternionf, float f, float g, float h, float i, CallbackInfo ci) {
        if (shouldTickSprite) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }
}