package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsTickerExtension;
import net.caffeinemc.mods.sodium.client.render.texture.TextureAtlasSpriteTickerExtension;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/renderer/texture/TextureAtlasSprite$1")
public abstract class TextureAtlasSpriteTickerImplMixin implements TextureAtlasSpriteTickerExtension {
    @Shadow
    @Final
    TextureAtlasSprite field_40555;
    @Shadow
    @Final
    SpriteTicker val$ticker;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void passTickerToSpriteContents(CallbackInfo ci) {
        ((SpriteContentsExtension) this.field_40555.contents()).sodium$setTicker(this);
    }

    @Override
    public void sodium$ensureUpload() {
        ((SpriteContentsTickerExtension) this.val$ticker).sodium$ensureUpload(this.field_40555.atlasLocation(), this.field_40555.getX(), this.field_40555.getY());
    }
}
