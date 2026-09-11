package net.caffeinemc.mods.sodium.client.render.frapi.wrapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import org.joml.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class QuadViewWrapper implements QuadView {
    private static final TriState[] TO_FABRIC = new TriState[] {
        TriState.TRUE, TriState.FALSE, TriState.DEFAULT
    };

    private QuadViewImpl quad;
    private final Vector4f posVec = new Vector4f();
    private final Vector3f normalVec = new Vector3f();
    private final Vector3f normalVec1 = new Vector3f();

    public QuadViewWrapper(QuadViewImpl quad) {
        this.quad = quad;
    }

    @Override
    public float x(int vertexIndex) {
        return this.quad.getX(vertexIndex);
    }

    @Override
    public float y(int vertexIndex) {
        return this.quad.getY(vertexIndex);
    }

    @Override
    public float z(int vertexIndex) {
        return this.quad.getZ(vertexIndex);
    }

    @Override
    public float posByIndex(int vertexIndex, int coordinateIndex) {
        return this.quad.posByIndex(vertexIndex, coordinateIndex);
    }

    @Override
    public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
        return this.quad.copyPos(vertexIndex, target);
    }

    @Override
    public int color(int vertexIndex) {
        return this.quad.getColor(vertexIndex);
    }

    @Override
    public float u(int vertexIndex) {
        return this.quad.getTexU(vertexIndex);
    }

    @Override
    public float v(int vertexIndex) {
        return this.quad.getTexV(vertexIndex);
    }

    @Override
    public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
        return this.quad.copyUv(vertexIndex, target);
    }

    @Override
    public int lightmap(int vertexIndex) {
        return this.quad.getLight(vertexIndex);
    }

    @Override
    public boolean hasNormal(int vertexIndex) {
        return this.quad.hasNormal(vertexIndex);
    }

    @Override
    public float normalX(int vertexIndex) {
        return this.quad.normalX(vertexIndex);
    }

    @Override
    public float normalY(int vertexIndex) {
        return this.quad.normalY(vertexIndex);
    }

    @Override
    public float normalZ(int vertexIndex) {
        return this.quad.normalZ(vertexIndex);
    }

    @Override
    public @Nullable Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
        return this.quad.copyNormal(vertexIndex, target);
    }

    @Override
    public Vector3fc faceNormal() {
        return this.quad.faceNormal();
    }

    @Override
    public @NonNull Direction lightFace() {
        return this.quad.getLightFace();
    }

    @Override
    public @Nullable Direction nominalFace() {
        return this.quad.getNominalFace();
    }

    @Override
    public @Nullable Direction cullFace() {
        return this.quad.getCullFace();
    }

    @Override
    public @Nullable ChunkSectionLayer chunkLayer() {
        return this.quad.getRenderType();
    }

    @Override
    public RenderType itemRenderType() {
        return this.quad.itemRenderType();
    }

    @Override
    public RenderType itemGlintRenderType() {
        return this.quad.itemGlintRenderType();
    }

    @Override
    public RenderType itemGlintSpecialRenderType() {
        return this.quad.itemGlintSpecialRenderType();
    }

    @Override
    public boolean emissive() {
        return this.quad.emissive();
    }

    @Override
    public @Nullable Direction shadeDirectionOverride() {
        return this.quad.shadeDirectionOverride();
    }

    @Override
    public TriState ambientOcclusion() {
        return TO_FABRIC[this.quad.ambientOcclusion().ordinal()];
    }

    @Override
    public ItemStackRenderState.@Nullable FoilType foilType() {
        return this.quad.glint();
    }

    @Override
    public ShadeMode shadeMode() {
        return this.quad.getShadeMode() == SodiumShadeMode.ENHANCED ? ShadeMode.ENHANCED : ShadeMode.VANILLA;
    }

    @Override
    public boolean animated() {
        return this.quad.animated();
    }

    public QuadAtlas atlas() {
        return this.quad.getQuadAtlas() == SodiumQuadAtlas.BLOCK ? QuadAtlas.BLOCK : QuadAtlas.ITEM;
    }

    @Override
    public int tintIndex() {
        return this.quad.getTintIndex();
    }

    @Override
    public int tag() {
        return this.quad.getTag();
    }

    @Override
    public final void buffer(int overlayCoords, VertexConsumer vertexConsumer) {
        if (!this.quad.hasVertexNormals()) {
            final Vector3fc faceNormal = this.faceNormal();

            for (int i = 0; i < 4; i++) {
                vertexConsumer.addVertex(this.x(i), this.y(i), this.z(i), this.color(i), this.u(i), this.v(i), overlayCoords, this.lightmap(i), faceNormal.x(), faceNormal.y(), faceNormal.z());
            }
        } else if (this.quad.hasAllVertexNormals()) {
            final Vector3f normalVec = this.normalVec;

            for (int i = 0; i < 4; i++) {
                this.copyNormal(i, normalVec);
                vertexConsumer.addVertex(this.x(i), this.y(i), this.z(i), this.color(i), this.u(i), this.v(i), overlayCoords, this.lightmap(i), normalVec.x(), normalVec.y(), normalVec.z());
            }
        } else {
            final Vector3f normalVec = this.normalVec;
            final Vector3fc faceNormal = this.faceNormal();

            for (int i = 0; i < 4; i++) {
                if (this.hasNormal(i)) {
                    this.copyNormal(i, normalVec);
                } else {
                    normalVec.set(faceNormal);
                }

                vertexConsumer.addVertex(this.x(i), this.y(i), this.z(i), this.color(i), this.u(i), this.v(i), overlayCoords, this.lightmap(i), normalVec.x(), normalVec.y(), normalVec.z());
            }
        }
    }

    // TODO: Optimize this (26.1)
    @Override
    public final void buffer(int overlayCoords, PoseStack.Pose pose, VertexConsumer vertexConsumer) {
        final Vector4f posVec = this.posVec;
        final Vector3f normalVec = this.normalVec;
        final Matrix4f posMatrix = pose.pose();

        if (!this.quad.hasVertexNormals()) {
            pose.transformNormal(this.faceNormal(), normalVec);

            for (int i = 0; i < 4; i++) {
                posVec.set(this.x(i), this.y(i), this.z(i), 1.0f);
                posVec.mul(posMatrix);
                vertexConsumer.addVertex(posVec.x(), posVec.y(), posVec.z(), this.color(i), this.u(i), this.v(i), overlayCoords, this.lightmap(i), normalVec.x(), normalVec.y(), normalVec.z());
            }
        } else if (this.quad.hasAllVertexNormals()) {
            for (int i = 0; i < 4; i++) {
                posVec.set(this.x(i), this.y(i), this.z(i), 1.0f);
                posVec.mul(posMatrix);
                this.copyNormal(i, normalVec);
                pose.transformNormal(normalVec, normalVec);
                vertexConsumer.addVertex(posVec.x(), posVec.y(), posVec.z(), this.color(i), this.u(i), this.v(i), overlayCoords, this.lightmap(i), normalVec.x(), normalVec.y(), normalVec.z());
            }
        } else {
            final Vector3f transformedFaceNormal = pose.transformNormal(this.faceNormal(), this.normalVec1);

            for (int i = 0; i < 4; i++) {
                posVec.set(this.x(i), this.y(i), this.z(i), 1.0f);
                posVec.mul(posMatrix);

                if (this.hasNormal(i)) {
                    this.copyNormal(i, normalVec);
                    pose.transformNormal(normalVec, normalVec);
                } else {
                    normalVec.set(transformedFaceNormal);
                }

                vertexConsumer.addVertex(posVec.x(), posVec.y(), posVec.z(), this.color(i), this.u(i), this.v(i), overlayCoords, this.lightmap(i), normalVec.x(), normalVec.y(), normalVec.z());
            }
        }
    }

    public QuadViewImpl getOriginal() {
        return this.quad;
    }

    protected void setDelegate(QuadViewImpl impl) {
        this.quad = impl;
    }
}
