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

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.helper.ListStorage;
import net.caffeinemc.mods.sodium.client.render.helper.TextureHelper;
import net.caffeinemc.mods.sodium.client.render.texture.SodiumSpriteFinder;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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

    private List<BlockModelPart> cachedList;

    @Override
    public List<BlockModelPart> clearAndGet() {
        if (cachedList == null) {
            cachedList = new ArrayList<>();
            return cachedList;
        }

        cachedList.clear();
        return cachedList;
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
        quad.setRenderType(null);
        quad.setDiffuseShade(true);
        quad.setAmbientOcclusion(TriState.DEFAULT);
        quad.setGlint(null);
        quad.setTintIndex(-1);
    }

    @Nullable
    public TextureAtlasSprite cachedSprite() {
        return cachedSprite;
    }

    public void cachedSprite(@Nullable TextureAtlasSprite sprite) {
        cachedSprite = sprite;
    }

    public TextureAtlasSprite sprite(SodiumSpriteFinder finder) {
        TextureAtlasSprite sprite = cachedSprite;

        if (sprite == null) {
            cachedSprite = sprite = finder.find(this);
        }

        return sprite;
    }

    public void clear() {
        System.arraycopy(DEFAULT, 0, data, baseIndex, EncodingFormat.TOTAL_STRIDE);
        isGeometryInvalid = true;
        nominalFace = null;
        cachedSprite(null);
    }

    @Override
    public void load() {
        super.load();
        cachedSprite(null);
    }

    public MutableQuadViewImpl setPos(int vertexIndex, float x, float y, float z) {
        final int index = baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X;
        data[index] = Float.floatToRawIntBits(x);
        data[index + 1] = Float.floatToRawIntBits(y);
        data[index + 2] = Float.floatToRawIntBits(z);
        isGeometryInvalid = true;
        return this;
    }

    public MutableQuadViewImpl setColor(int vertexIndex, int color) {
        data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_COLOR] = color;
        return this;
    }

    public MutableQuadViewImpl setUV(int vertexIndex, float u, float v) {
        final int i = baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_U;
        data[i] = Float.floatToRawIntBits(u);
        data[i + 1] = Float.floatToRawIntBits(v);
        cachedSprite(null);
        return this;
    }

    public MutableQuadViewImpl spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
        TextureHelper.bakeSprite(this, sprite, bakeFlags);
        cachedSprite(sprite);
        return this;
    }

    public MutableQuadViewImpl setLight(int vertexIndex, int lightmap) {
        data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_LIGHTMAP] = lightmap;
        return this;
    }

    protected void normalFlags(int flags) {
        data[baseIndex + HEADER_BITS] = EncodingFormat.normalFlags(data[baseIndex + HEADER_BITS], flags);
    }

    public MutableQuadViewImpl setNormal(int vertexIndex, float x, float y, float z) {
        normalFlags(normalFlags() | (1 << vertexIndex));
        data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_NORMAL] = NormI8.pack(x, y, z);
        return this;
    }

    /**
     * Internal helper method. Copies face normals to vertex normals lacking one.
     */
    public final void populateMissingNormals() {
        final int normalFlags = this.normalFlags();

        if (normalFlags == 0b1111) return;

        final int packedFaceNormal = packedFaceNormal();

        for (int v = 0; v < 4; v++) {
            if ((normalFlags & (1 << v)) == 0) {
                data[baseIndex + v * VERTEX_STRIDE + VERTEX_NORMAL] = packedFaceNormal;
            }
        }

        normalFlags(0b1111);
    }

    public final MutableQuadViewImpl setCullFace(@Nullable Direction face) {
        data[baseIndex + HEADER_BITS] = EncodingFormat.cullFace(data[baseIndex + HEADER_BITS], face);
        setNominalFace(face);
        return this;
    }

    public final MutableQuadViewImpl setNominalFace(@Nullable Direction face) {
        nominalFace = face;
        return this;
    }

    public MutableQuadViewImpl setRenderType(@Nullable ChunkSectionLayer renderLayer) {
        data[baseIndex + HEADER_BITS] = EncodingFormat.renderLayer(data[baseIndex + HEADER_BITS], renderLayer);
        return this;
    }

    public MutableQuadViewImpl setEmissive(boolean emissive) {
        data[baseIndex + HEADER_BITS] = EncodingFormat.emissive(data[baseIndex + HEADER_BITS], emissive);
        return this;
    }

    public MutableQuadViewImpl setDiffuseShade(boolean shade) {
        data[baseIndex + HEADER_BITS] = EncodingFormat.diffuseShade(data[baseIndex + HEADER_BITS], shade);
        return this;
    }

    public MutableQuadViewImpl setAmbientOcclusion(TriState ao) {
        Objects.requireNonNull(ao, "ambient occlusion TriState may not be null");
        data[baseIndex + HEADER_BITS] = EncodingFormat.ambientOcclusion(data[baseIndex + HEADER_BITS], ao);
        return this;
    }

    public MutableQuadViewImpl setGlint(@Nullable ItemStackRenderState.FoilType glint) {
        data[baseIndex + HEADER_BITS] = EncodingFormat.glint(data[baseIndex + HEADER_BITS], glint);
        return this;
    }

    public MutableQuadViewImpl setShadeMode(SodiumShadeMode mode) {
        Objects.requireNonNull(mode, "ShadeMode may not be null");
        data[baseIndex + HEADER_BITS] = EncodingFormat.shadeMode(data[baseIndex + HEADER_BITS], mode);
        return this;
    }

    public final MutableQuadViewImpl setTintIndex(int tintIndex) {
        data[baseIndex + HEADER_TINT_INDEX] = tintIndex;
        return this;
    }

    public final MutableQuadViewImpl setTag(int tag) {
        data[baseIndex + HEADER_TAG] = tag;
        return this;
    }

    public MutableQuadViewImpl copyFrom(QuadViewImpl q) {
        System.arraycopy(q.data, q.baseIndex, data, baseIndex, EncodingFormat.TOTAL_STRIDE);
        nominalFace = q.nominalFace;

        isGeometryInvalid = q.isGeometryInvalid;

        if (!isGeometryInvalid) {
            faceNormal.set(q.faceNormal);
        }

        if (q instanceof MutableQuadViewImpl mutableQuad) {
            cachedSprite(mutableQuad.cachedSprite());
        } else {
            cachedSprite(null);
        }

        return this;
    }

    /**
     * Does the same work as {@link #fromVanilla(int[], int)}, but does not mark the geometry as invalid
     * and does not clear the cached sprite.
     * Only use this if you are also setting the geometry and sprite.
     */
    private void fromVanillaInternal(int[] quadData, int startIndex) {
        System.arraycopy(quadData, startIndex, data, baseIndex + HEADER_STRIDE, EncodingFormat.VANILLA_QUAD_STRIDE);

        int colorIndex = baseIndex + VERTEX_COLOR;

        for (int i = 0; i < 4; i++) {
            data[colorIndex] = ColorHelper.fromVanillaColor(data[colorIndex]);
            colorIndex += VERTEX_STRIDE;
        }
    }

    public final MutableQuadViewImpl fromVanilla(int[] quadData, int startIndex) {
        fromVanillaInternal(quadData, startIndex);
        isGeometryInvalid = true;
        cachedSprite(null);
        return this;
    }

    public final MutableQuadViewImpl fromBakedQuad(BakedQuad quad) {
        fromVanillaInternal(quad.vertices(), 0);
        setNominalFace(quad.direction());
        setDiffuseShade(quad.shade());
        setTintIndex(quad.tintIndex());
        setAmbientOcclusion(((BakedQuadView) (Object) quad).hasAO() ? TriState.DEFAULT : TriState.FALSE); // TODO: TRUE, or DEFAULT?

        // Copy geometry cached inside the quad
        BakedQuadView bakedView = (BakedQuadView) (Object) quad;
        NormI8.unpack(bakedView.getFaceNormal(), faceNormal);
        data[baseIndex + HEADER_FACE_NORMAL] = bakedView.getFaceNormal();
        int headerBits = EncodingFormat.lightFace(data[baseIndex + HEADER_BITS], bakedView.getLightFace());
        headerBits = EncodingFormat.normalFace(headerBits, bakedView.getNormalFace());
        data[baseIndex + HEADER_BITS] = EncodingFormat.geometryFlags(headerBits, bakedView.getFlags());
        isGeometryInvalid = false;

        int lightEmission = quad.lightEmission();

        if (lightEmission > 0) {
            for (int i = 0; i < 4; i++) {
                setLight(i, LightTexture.lightCoordsWithEmission(getLight(i), lightEmission));
            }
        }

        cachedSprite(quad.sprite());
        return this;
    }

    /**
     * Emit the quad without clearing the underlying data.
     * Geometry is not guaranteed to be valid when called, but can be computed by calling {@link #computeGeometry()}.
     */
    public abstract void emitDirectly();
}
