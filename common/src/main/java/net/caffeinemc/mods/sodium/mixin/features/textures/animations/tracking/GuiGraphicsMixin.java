package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsMixin {

    @Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V",
            at = @At("HEAD"))
    private void preDrawSprite(RenderPipeline renderPipeline,
                               TextureAtlasSprite sprite,
                               int x,
                               int y,
                               int width,
                               int height,
                               int color,
                               CallbackInfo ci) {
        SpriteUtil.INSTANCE.markSpriteActive(sprite);
    }

    @Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V",
            at = @At("HEAD"))
    private void preDrawSprite(RenderPipeline renderPipeline,
                               TextureAtlasSprite sprite,
                               int spriteWidth,
                               int spriteHeight,
                               int textureX,
                               int textureY,
                               int x,
                               int y,
                               int width,
                               int height,
                               int color,
                               CallbackInfo ci) {
        SpriteUtil.INSTANCE.markSpriteActive(sprite);
    }
}
