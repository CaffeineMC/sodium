package net.caffeinemc.mods.sodium.client.render.frapi.wrapper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.QuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public class MutableQuadViewWrapper extends QuadViewWrapper implements QuadEmitter {
    protected static final QuadTransform NO_TRANSFORM = q -> true;
    private static final net.minecraft.util.TriState[] TO_SODIUM = new net.minecraft.util.TriState[] {
            net.minecraft.util.TriState.FALSE,
            net.minecraft.util.TriState.DEFAULT,
            net.minecraft.util.TriState.TRUE
    };
    private final MutableQuadViewImpl mutableQuad;

    protected QuadTransform activeTransform = NO_TRANSFORM;
    private final ObjectArrayList<QuadTransform> transformStack = new ObjectArrayList<>();
    private final QuadTransform stackTransform = q -> {
        int i = transformStack.size() - 1;

        while (i >= 0) {
            if (!transformStack.get(i--).transform(q)) {
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
    public QuadEmitter pos(int vertexIndex, float x, float y, float z) {
        mutableQuad.setPos(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public QuadEmitter color(int vertexIndex, int color) {
        mutableQuad.setColor(vertexIndex, color);
        return this;
    }

    @Override
    public QuadEmitter uv(int vertexIndex, float u, float v) {
        mutableQuad.setUV(vertexIndex, u, v);
        return this;
    }

    @Override
    public QuadEmitter lightmap(int vertexIndex, int lightmap) {
        mutableQuad.setLight(vertexIndex, lightmap);
        return this;
    }

    @Override
    public QuadEmitter normal(int vertexIndex, float x, float y, float z) {
        mutableQuad.setNormal(vertexIndex, x, y, z);
        return this;
    }

    @Override
    public QuadEmitter nominalFace(@Nullable Direction face) {
        mutableQuad.setNominalFace(face);
        return this;
    }

    @Override
    public QuadEmitter cullFace(@Nullable Direction face) {
        mutableQuad.setCullFace(face);
        return this;
    }

    @Override
    public QuadEmitter renderLayer(@Nullable ChunkSectionLayer renderLayer) {
        mutableQuad.setRenderType(renderLayer);
        return this;
    }

    @Override
    public QuadEmitter emissive(boolean emissive) {
        mutableQuad.setEmissive(emissive);
        return this;
    }

    @Override
    public QuadEmitter diffuseShade(boolean shade) {
        mutableQuad.setDiffuseShade(shade);
        return this;
    }

    @Override
    public QuadEmitter ambientOcclusion(TriState ao) {
        mutableQuad.setAmbientOcclusion(TO_SODIUM[ao.ordinal()]);
        return this;
    }

    @Override
    public QuadEmitter glint(ItemStackRenderState.@Nullable FoilType glint) {
        mutableQuad.setGlint(glint);
        return this;
    }

    @Override
    public QuadEmitter shadeMode(ShadeMode mode) {
        mutableQuad.setShadeMode(mode == ShadeMode.ENHANCED ? SodiumShadeMode.ENHANCED : SodiumShadeMode.VANILLA);
        return this;
    }

    @Override
    public QuadEmitter tintIndex(int tintIndex) {
        mutableQuad.setTintIndex(tintIndex);
        return this;
    }

    @Override
    public QuadEmitter tag(int tag) {
        mutableQuad.setTag(tag);
        return this;
    }

    @Override
    public QuadEmitter copyFrom(QuadView quad) {
        mutableQuad.copyFrom((QuadViewImpl) quad);
        return this;
    }

    @Override
    public QuadEmitter fromVanilla(int[] vertexData, int startIndex) {
        mutableQuad.fromVanilla(vertexData, startIndex);
        return this;
    }

    @Override
    public QuadEmitter fromBakedQuad(BakedQuad quad) {
        mutableQuad.fromBakedQuad(quad);
        return this;
    }

    @Override
    public void pushTransform(QuadTransform transform) {
        if (transform == null) {
            throw new NullPointerException("QuadTransform cannot be null!");
        }

        transformStack.push(transform);

        if (transformStack.size() == 1) {
            activeTransform = transform;
        } else if (transformStack.size() == 2) {
            activeTransform = stackTransform;
        }
    }

    @Override
    public void popTransform() {
        transformStack.pop();

        if (transformStack.isEmpty()) {
            activeTransform = NO_TRANSFORM;
        } else if (transformStack.size() == 1) {
            activeTransform = transformStack.getFirst();
        }
    }

    /**
     * Apply transforms and then if transforms return true, emit the quad without clearing the underlying data.
     */
    public final void transformAndEmit() {
        if (activeTransform.transform(this)) {
            mutableQuad.emitDirectly();
        }
    }

    @Override
    public final QuadEmitter emit() {
        transformAndEmit();
        mutableQuad.clear();
        return this;
    }

    public MutableQuadViewImpl getOriginal() {
        return mutableQuad;
    }
}
