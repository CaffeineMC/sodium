/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.caffeinemc.mods.sodium.client.render.model;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.helper.ListStorage;
import net.caffeinemc.mods.sodium.client.render.helper.TextureHelper;
import net.caffeinemc.mods.sodium.client.render.texture.SodiumSpriteFinder;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.caffeinemc.mods.sodium.client.render.model.EncodingFormat.*;

/**
 * Almost-concrete implementation of a mutable quad. The only missing part is {@link #emitDirectly()},
 * because that depends on where/how it is used. (Mesh encoding vs. render-time transformation).
 *
 * <p>In many cases an instance of this class is used as an "editor quad". The editor quad's
 * {@link #emitDirectly()} method calls some other internal method that transforms the quad
 * data and then buffers it. Transformations should be the same as they would be in a vanilla
 * render - the editor is serving mainly as a way to access vertex data without magical
 * numbers. It also allows for a consistent interface for those transformations.
 */
public abstract class MutableQuadViewImpl extends QuadViewImpl implements ListStorage {
    @Nullable
    private TextureAtlasSprite cachedSprite;

    private List<BlockStateModelPart> cachedList;

    @Override
    public List<BlockStateModelPart> clearAndGet() {
        if (this.cachedList == null) {
            this.cachedList = new ArrayList<>();
            return this.cachedList;
        }

        this.cachedList.clear();
        return this.cachedList;
    }

    /** Used for quick clearing of quad buffers. Implicitly has invalid geometry. */
    static final int[] DEFAULT = EMPTY.clone();

    static {
        MutableQuadViewImpl quad = new MutableQuadViewImpl() {
            @Override
            public void emitDirectly() {
                // This quad won't be emitted. It's only used to configure the default quad data.
            }
        };

        // Start with all zeroes
        quad.data = DEFAULT;
        // Apply non-zero defaults
        quad.setColor(0, -1);
        quad.setColor(1, -1);
        quad.setColor(2, -1);
        quad.setColor(3, -1);
        quad.setCullFace(null);
        quad.setRenderType(ChunkSectionLayer.CUTOUT);
        quad.setItemRenderType(ItemRenderType.DEFAULT.renderType);
        quad.setDiffuseShade(true);
        quad.setQuadAtlas(SodiumQuadAtlas.BLOCK);
        quad.setAmbientOcclusion(TriState.DEFAULT);
        quad.setGlint(null);
        quad.setTintIndex(-1);
    }

    @Nullable
    public TextureAtlasSprite cachedSprite() {
        return this.cachedSprite;
    }

    public void cachedSprite(@Nullable TextureAtlasSprite sprite) {
        this.cachedSprite = sprite;
    }

    public TextureAtlasSprite sprite(SodiumSpriteFinder finder) {
        TextureAtlasSprite sprite = this.cachedSprite;

        if (sprite == null) {
            sprite = finder.find(this);
            this.cachedSprite = sprite;
        }

        return sprite;
    }

    public void clear() {
        System.arraycopy(DEFAULT, 0, this.data, this.baseIndex, EncodingFormat.TOTAL_STRIDE);
        this.isGeometryInvalid = true;
        this.nominalFace = null;
        this.cachedSprite(null);
    }

    @Override
    public void load() {
        super.load();
        this.cachedSprite(null);
    }

    public MutableQuadViewImpl setPos(int vertexIndex, float x, float y, float z) {
        final int index = this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X;
        this.data[index] = Float.floatToRawIntBits(x);
        this.data[index + 1] = Float.floatToRawIntBits(y);
        this.data[index + 2] = Float.floatToRawIntBits(z);
        this.isGeometryInvalid = true;
        return this;
    }

    public MutableQuadViewImpl setColor(int vertexIndex, int color) {
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_COLOR] = color;
        return this;
    }

    public MutableQuadViewImpl setUV(int vertexIndex, float u, float v) {
        final int i = this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_U;
        this.data[i] = Float.floatToRawIntBits(u);
        this.data[i + 1] = Float.floatToRawIntBits(v);
        this.cachedSprite(null);
        return this;
    }

    public MutableQuadViewImpl spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
        TextureHelper.bakeSprite(this, sprite, bakeFlags);
        this.cachedSprite(sprite);
        return this;
    }

    public MutableQuadViewImpl setLight(int vertexIndex, int lightmap) {
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_LIGHTMAP] = lightmap;
        return this;
    }

    protected void normalFlags(int flags) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.normalFlags(this.data[this.baseIndex + HEADER_BITS], flags);
    }

    public MutableQuadViewImpl setNormal(int vertexIndex, float x, float y, float z) {
        this.normalFlags(this.normalFlags() | (1 << vertexIndex));
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_NORMAL] = NormI8.pack(x, y, z);
        return this;
    }

    /**
     * Internal helper method. Copies face normals to vertex normals lacking one.
     */
    public final void populateMissingNormals() {
        final int normalFlags = this.normalFlags();

        if (normalFlags == 0b1111) return;

        final int packedFaceNormal = this.packedFaceNormal();

        for (int v = 0; v < 4; v++) {
            if ((normalFlags & (1 << v)) == 0) {
                this.data[this.baseIndex + v * VERTEX_STRIDE + VERTEX_NORMAL] = packedFaceNormal;
            }
        }

        this.normalFlags(0b1111);
    }

    public final MutableQuadViewImpl setCullFace(@Nullable Direction face) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.cullFace(this.data[this.baseIndex + HEADER_BITS], face);
        this.setNominalFace(face);
        return this;
    }

    public final MutableQuadViewImpl setNominalFace(@Nullable Direction face) {
        this.nominalFace = face;
        return this;
    }

    public MutableQuadViewImpl setRenderType(@Nullable ChunkSectionLayer renderLayer) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.renderLayer(this.data[this.baseIndex + HEADER_BITS], renderLayer);
        return this;
    }

    public MutableQuadViewImpl setEmissive(boolean emissive) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.emissive(this.data[this.baseIndex + HEADER_BITS], emissive);
        return this;
    }

    public MutableQuadViewImpl setDiffuseShade(boolean shade) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.diffuseShade(this.data[this.baseIndex + HEADER_BITS], shade);
        return this;
    }

    public MutableQuadViewImpl setAmbientOcclusion(TriState ao) {
        Objects.requireNonNull(ao, "ambient occlusion TriState may not be null");
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.ambientOcclusion(this.data[this.baseIndex + HEADER_BITS], ao);
        return this;
    }

    public MutableQuadViewImpl setGlint(@Nullable ItemStackRenderState.FoilType glint) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.glint(this.data[this.baseIndex + HEADER_BITS], glint);
        return this;
    }

    public MutableQuadViewImpl setShadeMode(SodiumShadeMode mode) {
        Objects.requireNonNull(mode, "ShadeMode may not be null");
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.shadeMode(this.data[this.baseIndex + HEADER_BITS], mode);
        return this;
    }

    public final MutableQuadViewImpl setTintIndex(int tintIndex) {
        this.data[this.baseIndex + HEADER_TINT_INDEX] = tintIndex;
        return this;
    }

    public final MutableQuadViewImpl setQuadAtlas(SodiumQuadAtlas atlas) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.quadAtlas(this.data[this.baseIndex + HEADER_BITS], atlas);
        return this;
    }

    public final MutableQuadViewImpl setTag(int tag) {
        this.data[this.baseIndex + HEADER_TAG] = tag;
        return this;
    }

    public MutableQuadViewImpl copyFrom(QuadViewImpl q) {
        System.arraycopy(q.data, q.baseIndex, this.data, this.baseIndex, EncodingFormat.TOTAL_STRIDE);
        this.nominalFace = q.nominalFace;

        this.isGeometryInvalid = q.isGeometryInvalid;

        if (!this.isGeometryInvalid) {
            this.faceNormal.set(q.faceNormal);
        }

        if (q instanceof MutableQuadViewImpl mutableQuad) {
            this.cachedSprite(mutableQuad.cachedSprite());
        } else {
            this.cachedSprite(null);
        }


        return this;
    }

    private void fromVanillaInternal(BakedQuadView quadData) {
        boolean hasNormals = false;

        for (int i = 0; i < 4; i++) {
            this.setPos(i, quadData.getX(i), quadData.getY(i), quadData.getZ(i));
            this.setColor(i, quadData.getColor(i));
            this.setUV(i, quadData.getTexU(i), quadData.getTexV(i));
            this.setLight(i, quadData.getMaxLightQuad(i));

            int normal = quadData.getVertexNormal(i);
            if (normal != 0) hasNormals = true;
            this.setNormal(i, NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
        }

        this.normalFlags(hasNormals ? 0b1111 : 0);
    }

    public final MutableQuadViewImpl fromBakedQuad(BakedQuad quad) {
        this.fromVanillaInternal(((BakedQuadView) (Object) quad));
        this.setNominalFace(quad.direction());
        this.setDiffuseShade(quad.materialInfo().shade());
        this.setTintIndex(quad.materialInfo().tintIndex());
        this.setAmbientOcclusion(((BakedQuadView) (Object) quad).hasAO() ? TriState.DEFAULT : TriState.FALSE);
        this.setItemRenderType(quad.materialInfo().itemRenderType());
        this.setRenderType(quad.materialInfo().layer());
        this.setAnimated(quad.materialInfo().sprite().contents().isAnimated());
        this.setEmissive(quad.materialInfo().lightEmission() == 15);

        // Copy geometry cached inside the quad
        BakedQuadView bakedView = (BakedQuadView) (Object) quad;
        NormI8.unpack(bakedView.getFaceNormal(), this.faceNormal);
        this.data[this.baseIndex + HEADER_FACE_NORMAL] = bakedView.getFaceNormal();
        int headerBits = EncodingFormat.lightFace(this.data[this.baseIndex + HEADER_BITS], bakedView.getLightFace());
        headerBits = EncodingFormat.normalFace(headerBits, bakedView.getNormalFace());
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.geometryFlags(headerBits, bakedView.getFlags());
        this.isGeometryInvalid = false;

        SodiumQuadAtlas atlas = SodiumQuadAtlas.of(quad.materialInfo().sprite().atlasLocation());

        if (atlas == null) {
            atlas = SodiumQuadAtlas.BLOCK;
        }

        this.setQuadAtlas(atlas);
        this.cachedSprite(quad.materialInfo().sprite());
        return this;
    }

    /**
     * Emit the quad without clearing the underlying data.
     * Geometry is not guaranteed to be valid when called, but can be computed by calling {@link #computeGeometry()}.
     */
    public abstract void emitDirectly();

    public MutableQuadViewImpl translate(float x, float y, float z) {
        for (int i = 0; i < 4; i++) {
            this.setPos(i, this.getX(i) + x, this.getY(i) + y, this.getZ(i) + z);
        }

        return this;
    }

    public MutableQuadViewImpl setItemRenderType(RenderType renderType) {
        ItemRenderType enumValue = ItemRenderType.RENDER_TYPE_2_ENUM.get(renderType);

        if (enumValue != null) {
            this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.itemRenderType(this.data[this.baseIndex + HEADER_BITS], enumValue);
        }

        return this;
    }

    public MutableQuadViewImpl setAnimated(boolean b) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.animated(this.data[this.baseIndex + HEADER_BITS], b);
        return this;
    }
}
