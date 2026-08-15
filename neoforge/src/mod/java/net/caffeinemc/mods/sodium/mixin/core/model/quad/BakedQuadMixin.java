package net.caffeinemc.mods.sodium.mixin.core.model.quad;

import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
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
    private BakedNormals bakedNormals;
    @Shadow
    @Final
    private BakedColors bakedColors;
    @Shadow
    @Final
    private BakedQuad.MaterialInfo materialInfo;

    @Unique
    private int sodium$flags;
    @Unique
    private int sodium$normal;
    @Unique
    private ModelQuadFacing sodium$normalFace = null;

    @Inject(method = "<init>(Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;Lorg/joml/Vector3fc;JJJJLnet/minecraft/core/Direction;Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;Lnet/neoforged/neoforge/client/model/quad/BakedNormals;Lnet/neoforged/neoforge/client/model/quad/BakedColors;)V",
            at = @At("RETURN"))
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
                      BakedNormals bakedNormals,
                      BakedColors bakedColors,
                      CallbackInfo ci) {
        this.sodium$normal = this.calculateNormal();
        this.sodium$normalFace = ModelQuadFacing.fromPackedNormal(this.sodium$normal);

        this.sodium$flags = ModelQuadFlags.getQuadFlags(this, direction);
    }

    @Override
    public float getX(int idx) {
        return this.position(idx).x();
    }

    @Override
    public float getY(int idx) {
        return this.position(idx).y();
    }

    @Override
    public float getZ(int idx) {
        return this.position(idx).z();
    }

    @Override
    public int getColor(int idx) {
        return this.bakedColors.color(idx); // default is -1 for now
    }

    @Override
    public int getVertexNormal(int idx) {
        //this.vertices[ModelQuadUtil.vertexOffset(idx) + ModelQuadUtil.NORMAL_INDEX];
        return this.bakedNormals == null || this.bakedNormals == BakedNormals.UNSPECIFIED ? 0 : this.bakedNormals.normal(idx);
    }

    @Override
    public int getLight(int idx) {
        return 0;//this.vertices[ModelQuadUtil.vertexOffset(idx) + ModelQuadUtil.LIGHT_INDEX];
    }

    @Override
    public TextureAtlasSprite getSprite() {
        return this.materialInfo.sprite();
    }

    @Override
    public float getTexU(int idx) {
        return UVPair.unpackU(this.packedUV(idx));
    }

    @Override
    public float getTexV(int idx) {
        return UVPair.unpackV(this.packedUV(idx));
    }

    @Override
    public int getFlags() {
        return this.sodium$flags;
    }

    @Override
    public int getTintIndex() {
        return this.materialInfo.tintIndex();
    }

    @Override
    public ModelQuadFacing getNormalFace() {
        return this.sodium$normalFace;
    }

    @Override
    public int getFaceNormal() {
        return this.sodium$normal;
    }

    @Override
    public Direction getLightFace() {
        return this.direction;
    }

    @Override
    public int getMaxLightQuad(int idx) {
        return LightCoordsUtil.lightCoordsWithEmission(this.getLight(idx), this.materialInfo.lightEmission());
    }

    @Override
    public int getLightEmission() {
        return this.materialInfo.lightEmission();
    }

    @Override
    public boolean hasShade() {
        return this.materialInfo.shade();
    }

    @Override
    public boolean hasAO() {
        return this.materialInfo.ambientOcclusion();
    }
}
