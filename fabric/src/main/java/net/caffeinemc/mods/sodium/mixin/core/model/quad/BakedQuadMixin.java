package net.caffeinemc.mods.sodium.mixin.core.model.quad;

import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements BakedQuadView {
    @Shadow
    public abstract Vector3fc position(int vertex);

    @Shadow
    public abstract long packedUV(int vertex);

    @Shadow
    @Final
    private Direction direction;
    @Shadow
    @Final
    private BakedQuad.MaterialInfo materialInfo;
    @Unique
    private int flags;

    @Unique
    private int normal;

    @Unique
    private ModelQuadFacing normalFace = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Vector3fc position0,
                      Vector3fc position1,
                      Vector3fc position2,
                      Vector3fc position3,
                      long packedUV0,
                      long packedUV1,
                      long packedUV2,
                      long packedUV3,
                      Direction direction,
                      BakedQuad.MaterialInfo materialInfo,
                      CallbackInfo ci) {
        this.normal = this.sodium$calculateNormal();
        this.normalFace = ModelQuadFacing.fromPackedNormal(this.normal);

        this.flags = ModelQuadFlags.getQuadFlags(this, direction);
    }

    @Override
    public float sodium$getX(int idx) {
        return this.position(idx).x();
    }

    @Override
    public float sodium$getY(int idx) {
        return this.position(idx).y();
    }

    @Override
    public float sodium$getZ(int idx) {
        return this.position(idx).z();
    }

    @Override
    public int sodium$getColor(int idx) {
        return 0xFFFFFFFF;
    }

    @Override
    public int sodium$getVertexNormal(int idx) {
        return this.normal;
    }

    @Override
    public int sodium$getLight(int idx) {
        return 0;
    }

    @Override
    public TextureAtlasSprite sodium$getSprite() {
        return this.materialInfo.sprite();
    }

    @Override
    public float sodium$getTexU(int idx) {
        return UVPair.unpackU(this.packedUV(idx));
    }

    @Override
    public float sodium$getTexV(int idx) {
        return UVPair.unpackV(this.packedUV(idx));
    }

    @Override
    public int sodium$getFlags() {
        return this.flags;
    }

    @Override
    public int sodium$getTintIndex() {
        return this.materialInfo.tintIndex();
    }

    @Override
    public ModelQuadFacing sodium$getNormalFace() {
        return this.normalFace;
    }

    @Override
    public int sodium$getFaceNormal() {
        return this.normal;
    }

    @Override
    public Direction sodium$getLightFace() {
        return this.direction;
    }

    @Override
    public int sodium$getMaxLightQuad(int idx) {
        return LightCoordsUtil.lightCoordsWithEmission(this.sodium$getLight(idx), this.materialInfo.lightEmission());
    }

    @Override
    public int sodium$getLightEmission() {
        return this.materialInfo.lightEmission();
    }

    @Override
    public boolean sodium$hasShade() {
        return true;
    }

    @Override
    public boolean sodium$hasAO() {
        return true;
    }
}
