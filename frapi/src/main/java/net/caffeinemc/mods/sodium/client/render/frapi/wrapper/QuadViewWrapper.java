package net.caffeinemc.mods.sodium.client.render.frapi.wrapper;

import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadAtlas;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class QuadViewWrapper implements QuadView {
    private static final TriState[] TO_FABRIC = new TriState[] {
        TriState.TRUE, TriState.FALSE, TriState.DEFAULT
    };

    private final QuadViewImpl quad;

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
    public @Nullable ChunkSectionLayer renderLayer() {
        return this.quad.getRenderType();
    }

    @Override
    public boolean emissive() {
        return this.quad.emissive();
    }

    @Override
    public boolean diffuseShade() {
        return this.quad.diffuseShade();
    }

    @Override
    public TriState ambientOcclusion() {
        return TO_FABRIC[this.quad.ambientOcclusion().ordinal()];
    }

    @Override
    public ItemStackRenderState.@Nullable FoilType glint() {
        return this.quad.glint();
    }

    @Override
    public ShadeMode shadeMode() {
        return this.quad.getShadeMode() == SodiumShadeMode.ENHANCED ? ShadeMode.ENHANCED : ShadeMode.VANILLA;
    }

    @Override
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

    public QuadViewImpl getOriginal() {
        return this.quad;
    }
}
