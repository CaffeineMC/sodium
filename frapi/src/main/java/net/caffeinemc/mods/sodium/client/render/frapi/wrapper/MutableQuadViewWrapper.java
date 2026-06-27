package net.caffeinemc.mods.sodium.client.render.frapi.wrapper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.*;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public class MutableQuadViewWrapper extends QuadViewWrapper implements QuadEmitter {
    protected static final QuadTransform NO_TRANSFORM = q -> true;
    private static final net.minecraft.util.TriState[] TO_SODIUM = new net.minecraft.util.TriState[] {
            net.minecraft.util.TriState.FALSE,
            net.minecraft.util.TriState.DEFAULT,
            net.minecraft.util.TriState.TRUE
    };
    private MutableQuadViewImpl mutableQuad;

    protected QuadTransform activeTransform = NO_TRANSFORM;
    private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();
    private final QuadTransform stackTransform = q -> {
        int i = this.transformStack.size() - 1;

        while (i >= 0) {
            if (!this.transformStack.get(i--).transform(q)) {
                return false;
            }
        }

        return true;
    };

    public MutableQuadViewWrapper(MutableQuadViewImpl quad) {
        super(quad);
        this.mutableQuad = quad;
    }

    @Override
    public QuadEmitter translate(float x, float y, float z) {
        this.mutableQuad.translate(x, y, z);
        return this;
    }

    @Override
    public QuadEmitter pos(int vertexIndex, float x, float y, float z) {
        this.mutableQuad.setPos(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public QuadEmitter color(int vertexIndex, int color) {
        this.mutableQuad.setColor(vertexIndex, color);
        return this;
    }

    @Override
    public QuadEmitter uv(int vertexIndex, float u, float v) {
        this.mutableQuad.setUV(vertexIndex, u, v);
        return this;
    }

    @Override
    public QuadEmitter lightmap(int vertexIndex, int lightmap) {
        this.mutableQuad.setLight(vertexIndex, lightmap);
        return this;
    }

    @Override
    public QuadEmitter normal(int vertexIndex, float x, float y, float z) {
        this.mutableQuad.setNormal(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public QuadEmitter nominalFace(@Nullable Direction face) {
        this.mutableQuad.setNominalFace(face);
        return this;
    }

    @Override
    public QuadEmitter cullFace(@Nullable Direction face) {
        this.mutableQuad.setCullFace(face);
        return this;
    }

    @Override
    public QuadEmitter chunkLayer(@Nullable ChunkSectionLayer renderLayer) {
        this.mutableQuad.setRenderType(renderLayer);
        return this;
    }

    @Override
    public QuadEmitter itemRenderType(RenderType renderType) {
        this.mutableQuad.setItemRenderType(renderType);
        return this;
    }

    @Override
    public QuadEmitter emissive(boolean emissive) {
        this.mutableQuad.setEmissive(emissive);
        return this;
    }

    @Override
    public QuadEmitter diffuseShade(boolean shade) {
        this.mutableQuad.setDiffuseShade(shade);
        return this;
    }

    @Override
    public QuadEmitter ambientOcclusion(TriState ao) {
        this.mutableQuad.setAmbientOcclusion(TO_SODIUM[ao.ordinal()]);
        return this;
    }

    @Override
    public QuadEmitter foilType(ItemStackRenderState.@Nullable FoilType glint) {
        this.mutableQuad.setGlint(glint);
        return this;
    }

    @Override
    public QuadEmitter shadeMode(ShadeMode mode) {
        this.mutableQuad.setShadeMode(mode == ShadeMode.ENHANCED ? SodiumShadeMode.ENHANCED : SodiumShadeMode.VANILLA);
        return this;
    }

    @Override
    public QuadEmitter animated(boolean b) {
        this.mutableQuad.setAnimated(b);
        return this;
    }

    @Override
    public QuadEmitter atlas(QuadAtlas quadAtlas) {
        this.mutableQuad.setQuadAtlas(quadAtlas == QuadAtlas.BLOCK ? SodiumQuadAtlas.BLOCK : SodiumQuadAtlas.ITEM);
        return this;
    }

    @Override
    public QuadEmitter tintIndex(int tintIndex) {
        this.mutableQuad.setTintIndex(tintIndex);
        return this;
    }

    @Override
    public QuadEmitter tag(int tag) {
        this.mutableQuad.setTag(tag);
        return this;
    }

    @Override
    public QuadEmitter copyFrom(QuadView quad) {
        this.mutableQuad.copyFrom(((QuadViewWrapper) quad).getOriginal());
        return this;
    }

    @Override
    public QuadEmitter fromBakedQuad(BakedQuad quad) {
        this.mutableQuad.fromBakedQuad(quad);
        return this;
    }

    @Override
    public QuadEmitter clear() {
        this.mutableQuad.clear();
        return this;
    }

    @Override
    public void pushTransform(QuadTransform transform) {
        if (transform == null) {
            throw new NullPointerException("QuadTransform cannot be null!");
        }

        this.transformStack.push(transform);

        if (this.transformStack.size() == 1) {
            this.activeTransform = transform;
        } else if (this.transformStack.size() == 2) {
            this.activeTransform = this.stackTransform;
        }
    }

    @Override
    public void popTransform() {
        this.transformStack.pop();

        if (this.transformStack.isEmpty()) {
            this.activeTransform = NO_TRANSFORM;
        } else if (this.transformStack.size() == 1) {
            this.activeTransform = this.transformStack.getFirst();
        }
    }


    /**
     * Apply transforms and then if transforms return true, emit the quad without clearing the underlying data.
     */
    public final void transformAndEmit() {
        if (this.activeTransform.transform(this)) {
            this.mutableQuad.emitDirectly();
        }
    }

    @Override
    public final QuadEmitter emit() {
        this.transformAndEmit();
        this.mutableQuad.clear();
        return this;
    }

    public MutableQuadViewImpl getOriginal() {
        return this.mutableQuad;
    }

    @Override
    public QuadAtlas atlas() {
        return this.mutableQuad.getQuadAtlas() == SodiumQuadAtlas.BLOCK ? QuadAtlas.BLOCK : QuadAtlas.ITEM;
    }

    public void setDelegate(MutableQuadViewImpl impl) {
        super.setDelegate(impl);
        this.mutableQuad = impl;
    }
}
