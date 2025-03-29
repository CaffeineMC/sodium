package net.caffeinemc.mods.sodium.mixin.features.textures.animations.tracking;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteContentsExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.Ticker.class)
public class SpriteContentsTickerMixin {
    @Shadow
    int frame;
    @Shadow
    int subFrame;
    @Shadow
    @Final
    SpriteContents.AnimatedTexture animationInfo;
    @Shadow
    @Final
    @Nullable
    private SpriteContents.InterpolationData interpolationData;

    @Unique
    private SpriteContents parent;

    @Unique
    private boolean skippedUpload;
    @Unique
    private int uploadedFrame;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents spriteContents, SpriteContents.AnimatedTexture animation, SpriteContents.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = spriteContents;
    }

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void preTick(int x, int y, CallbackInfo ci) {
        SpriteContentsExtension parent = (SpriteContentsExtension) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            if (!this.skippedUpload) {
                this.uploadedFrame = this.frame;
                this.skippedUpload = true;
            }
            this.tickWithoutUpload();
            ci.cancel();
            return;
        }

        // make sure image is uploaded immediately once it becomes visible again
        // the vanilla logic would only update it on the next frame increment
        if (this.skippedUpload) {
            this.tickWithoutUpload();
            this.ensureUpload(x, y);
            this.skippedUpload = false;
            ci.cancel();
        }
    }

    @Unique
    private void tickWithoutUpload() {
        this.subFrame++;
        List<SpriteContents.FrameInfo> frames = ((AnimatedTextureAccessor) this.animationInfo).sodium$getFrames();
        if (this.subFrame >= ((SpriteContentsFrameInfoAccessor) (Object) frames.get(this.frame)).getTime()) {
            this.frame = (this.frame + 1) % frames.size();
            this.subFrame = 0;
        }
    }

    @Unique
    private void ensureUpload(int x, int y) {
        List<SpriteContents.FrameInfo> frames = ((AnimatedTextureAccessor) this.animationInfo).sodium$getFrames();
        int index = frames.get(this.frame).index();
        if (this.interpolationData != null) {
            if (this.subFrame == 0) {
                ((AnimatedTextureAccessor) this.animationInfo).sodium$uploadFrame(x, y, index);
            } else {
                ((InterpolationDataAccessor) (Object) this.interpolationData).sodium$uploadInterpolatedFrame(x, y, (SpriteContents.Ticker) (Object) this);
            }
        } else {
            int uploadedIndex = frames.get(this.uploadedFrame).index();
            if (index != uploadedIndex) {
                ((AnimatedTextureAccessor) this.animationInfo).sodium$uploadFrame(x, y, index);
            }
        }
    }

    @Inject(method = "tickAndUpload", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteContentsExtension parent = (SpriteContentsExtension) this.parent;
        parent.sodium$setActive(false);
    }
}
