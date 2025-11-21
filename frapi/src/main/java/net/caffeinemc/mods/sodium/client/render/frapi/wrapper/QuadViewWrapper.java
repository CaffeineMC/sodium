package net.caffeinemc.mods.sodium.client.render.frapi.wrapper;

import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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
        return quad.getX(vertexIndex);
    }

    @Override
    public float y(int vertexIndex) {
        return quad.getY(vertexIndex);
    }

    @Override
    public float z(int vertexIndex) {
        return quad.getZ(vertexIndex);
    }

    @Override
    public float posByIndex(int vertexIndex, int coordinateIndex) {
        return quad.posByIndex(vertexIndex, coordinateIndex);
    }

    @Override
    public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
        return quad.copyPos(vertexIndex, target);
    }

    @Override
    public int color(int vertexIndex) {
        return quad.getColor(vertexIndex);
    }

    @Override
    public float u(int vertexIndex) {
        return quad.getTexU(vertexIndex);
    }

    @Override
    public float v(int vertexIndex) {
        return quad.getTexV(vertexIndex);
    }

    @Override
    public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
        return quad.copyUv(vertexIndex, target);
    }

    @Override
    public int lightmap(int vertexIndex) {
        return quad.getLight(vertexIndex);
    }

    @Override
    public boolean hasNormal(int vertexIndex) {
        return quad.hasNormal(vertexIndex);
    }

    @Override
    public float normalX(int vertexIndex) {
        return quad.normalX(vertexIndex);
    }

    @Override
    public float normalY(int vertexIndex) {
        return quad.normalY(vertexIndex);
    }

    @Override
    public float normalZ(int vertexIndex) {
        return quad.normalZ(vertexIndex);
    }

    @Override
    public @Nullable Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
        return quad.copyNormal(vertexIndex, target);
    }

    @Override
    public Vector3fc faceNormal() {
        return quad.faceNormal();
    }

    @Override
    public @NonNull Direction lightFace() {
        return quad.getLightFace();
    }

    @Override
    public @Nullable Direction nominalFace() {
        return quad.getNominalFace();
    }

    @Override
    public @Nullable Direction cullFace() {
        return quad.getCullFace();
    }

    @Override
    public @Nullable ChunkSectionLayer renderLayer() {
        return quad.getRenderType();
    }

    @Override
    public boolean emissive() {
        return quad.emissive();
    }

    @Override
    public boolean diffuseShade() {
        return quad.diffuseShade();
    }

    @Override
    public TriState ambientOcclusion() {
        return TO_FABRIC[quad.ambientOcclusion().ordinal()];
    }

    @Override
    public ItemStackRenderState.@Nullable FoilType glint() {
        return quad.glint();
    }

    @Override
    public ShadeMode shadeMode() {
        return quad.getShadeMode() == SodiumShadeMode.ENHANCED ? ShadeMode.ENHANCED : ShadeMode.VANILLA;
    }

    @Override
    public int tintIndex() {
        return quad.getTintIndex();
    }

    @Override
    public int tag() {
        return quad.getTag();
    }

    @Override
    public void toVanilla(int[] target, int startIndex) {
        quad.toVanilla(target, startIndex);
    }

    public QuadViewImpl getOriginal() {
        return quad;
    }
}
