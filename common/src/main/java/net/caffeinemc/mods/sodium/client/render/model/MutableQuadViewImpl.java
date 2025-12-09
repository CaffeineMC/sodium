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
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
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


    static {
        MutableQuadViewImpl quad = new MutableQuadViewImpl() {
            @Override
            public void emitDirectly() {
                // This quad won't be emitted. It's only used to configure the default quad data.
            }
        };

        // Start with all zeroes
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
        for (int i = 0; i < 4; i++) {
            positions[i].set(0, 0, 0);
            colors[i] = 0;
            uv[i] = 0;
            light[i] = 0;
        }
        maxLightEmission = 0;
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
        positions[vertexIndex].set(x, y, z);
        isGeometryInvalid = true;
        return this;
    }

    public MutableQuadViewImpl setColor(int vertexIndex, int color) {
        colors[vertexIndex] = color;
        return this;
    }

    public MutableQuadViewImpl setUV(int vertexIndex, float u, float v) {
        uv[vertexIndex] = UVPair.pack(u, v);
        cachedSprite(null);
        return this;
    }

    public MutableQuadViewImpl spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
        TextureHelper.bakeSprite(this, sprite, bakeFlags);
        cachedSprite(sprite);
        return this;
    }

    public MutableQuadViewImpl setLight(int vertexIndex, int lightmap) {
        light[vertexIndex] = lightmap;
        return this;
    }

    protected void normalFlags(int flags) {
        header = EncodingFormat.normalFlags(header, flags);
    }

    public MutableQuadViewImpl setNormal(int vertexIndex, float x, float y, float z) {
        normalFlags(normalFlags() | (1 << vertexIndex));
        throw new IllegalStateException("Not implemented as of 1.21.11"); // data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_NORMAL] = NormI8.pack(x, y, z);
    }

    /**
     * Internal helper method. Copies face normals to vertex normals lacking one.
     */
    public final void populateMissingNormals() {
        throw new IllegalStateException("Not implemented as of 1.21.11"); /*
        final int normalFlags = this.normalFlags();

        if (normalFlags == 0b1111) return;

        final int packedFaceNormal = packedFaceNormal();

        for (int v = 0; v < 4; v++) {
            if ((normalFlags & (1 << v)) == 0) {
                //data[baseIndex + v * VERTEX_STRIDE + VERTEX_NORMAL] = packedFaceNormal;
            }
        }

        normalFlags(0b1111);*/
    }

    public final MutableQuadViewImpl setCullFace(@Nullable Direction face) {
        header = EncodingFormat.cullFace(header, face);
        setNominalFace(face);
        return this;
    }

    public final MutableQuadViewImpl setNominalFace(@Nullable Direction face) {
        nominalFace = face;
        return this;
    }

    public MutableQuadViewImpl setRenderType(@Nullable ChunkSectionLayer renderLayer) {
        header = EncodingFormat.renderLayer(header, renderLayer);
        return this;
    }

    public MutableQuadViewImpl setEmissive(boolean emissive) {
        header = EncodingFormat.emissive(header, emissive);
        return this;
    }

    public MutableQuadViewImpl setDiffuseShade(boolean shade) {
        header = EncodingFormat.diffuseShade(header, shade);
        return this;
    }

    public MutableQuadViewImpl setAmbientOcclusion(TriState ao) {
        Objects.requireNonNull(ao, "ambient occlusion TriState may not be null");
        header = EncodingFormat.ambientOcclusion(header, ao);
        return this;
    }

    public MutableQuadViewImpl setGlint(ItemStackRenderState.@Nullable FoilType glint) {
        header = EncodingFormat.glint(header, glint);
        return this;
    }

    public MutableQuadViewImpl setShadeMode(SodiumShadeMode mode) {
        Objects.requireNonNull(mode, "ShadeMode may not be null");
        header = EncodingFormat.shadeMode(header, mode);
        return this;
    }

    public final MutableQuadViewImpl setTintIndex(int tintIndex) {
        this.tintIndex = tintIndex;
        return this;
    }

    public final MutableQuadViewImpl setTag(int tag) {
        this.tag = tag;
        return this;
    }

    public MutableQuadViewImpl copyFrom(QuadViewImpl q) {
        //System.arraycopy(q.data, q.baseIndex, data, baseIndex, EncodingFormat.TOTAL_STRIDE);
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

        throw new IllegalStateException("Not implemented as of 1.21.11");
    }

    public final MutableQuadViewImpl fromBakedQuad(BakedQuad quad) {
        for (int i = 0; i < 4; i++) {
            positions[i].set(quad.position(i));
            uv[i] = quad.packedUV(i);
            colors[i] = 0xFFFFFFFF;
        }
        Arrays.fill(light, 0);
        setNominalFace(quad.direction());
        setDiffuseShade(quad.shade());
        setTintIndex(quad.tintIndex());
        setAmbientOcclusion(((BakedQuadView) (Object) quad).hasAO() ? TriState.DEFAULT : TriState.FALSE); // TODO: TRUE, or DEFAULT?

        // Copy geometry cached inside the quad
        BakedQuadView bakedView = (BakedQuadView) (Object) quad;
        NormI8.unpack(bakedView.getFaceNormal(), faceNormal);
        packedNormal = bakedView.getFaceNormal();
        int headerBits = EncodingFormat.lightFace(header, bakedView.getLightFace());
        headerBits = EncodingFormat.normalFace(headerBits, bakedView.getNormalFace());
        header = EncodingFormat.geometryFlags(headerBits, bakedView.getFlags());
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
