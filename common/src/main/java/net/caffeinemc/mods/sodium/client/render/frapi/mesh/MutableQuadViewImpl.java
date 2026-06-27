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

package net.caffeinemc.mods.sodium.client.render.frapi.mesh;

import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.TextureHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.material.RenderMaterialImpl;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import static net.caffeinemc.mods.sodium.client.render.frapi.mesh.EncodingFormat.*;

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
public abstract class MutableQuadViewImpl extends QuadViewImpl implements QuadEmitter {
    @Nullable
    private TextureAtlasSprite cachedSprite;

    @Nullable
    public TextureAtlasSprite cachedSprite() {
        return this.cachedSprite;
    }

    public void cachedSprite(@Nullable TextureAtlasSprite sprite) {
        this.cachedSprite = sprite;
    }

    public TextureAtlasSprite sprite(SpriteFinder finder) {
        TextureAtlasSprite sprite = this.cachedSprite;

        if (sprite == null) {
            this.cachedSprite = sprite = finder.find(this);
        }

        return sprite;
    }

    public void clear() {
        System.arraycopy(EMPTY, 0, this.data, this.baseIndex, EncodingFormat.TOTAL_STRIDE);
        this.isGeometryInvalid = true;
        this.nominalFace = null;
        this.normalFlags(0);
        this.tag(0);
        this.colorIndex(-1);
        this.cullFace(null);
        this.material(SodiumRenderer.STANDARD_MATERIAL);
        this.cachedSprite(null);
    }

    @Override
    public void load() {
        super.load();
        this.cachedSprite(null);
    }

    @Override
    public MutableQuadViewImpl pos(int vertexIndex, float x, float y, float z) {
        final int index = this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X;
        this.data[index] = Float.floatToRawIntBits(x);
        this.data[index + 1] = Float.floatToRawIntBits(y);
        this.data[index + 2] = Float.floatToRawIntBits(z);
        this.isGeometryInvalid = true;
        return this;
    }

    @Override
    public MutableQuadViewImpl color(int vertexIndex, int color) {
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_COLOR] = color;
        return this;
    }

    @Override
    public MutableQuadViewImpl uv(int vertexIndex, float u, float v) {
        final int i = this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_U;
        this.data[i] = Float.floatToRawIntBits(u);
        this.data[i + 1] = Float.floatToRawIntBits(v);
        this.cachedSprite(null);
        return this;
    }

    @Override
    public MutableQuadViewImpl spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
        TextureHelper.bakeSprite(this, sprite, bakeFlags);
        this.cachedSprite(sprite);
        return this;
    }

    @Override
    public MutableQuadViewImpl lightmap(int vertexIndex, int lightmap) {
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_LIGHTMAP] = lightmap;
        return this;
    }

    protected void normalFlags(int flags) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.normalFlags(this.data[this.baseIndex + HEADER_BITS], flags);
    }

    @Override
    public MutableQuadViewImpl normal(int vertexIndex, float x, float y, float z) {
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

    @Override
    public final MutableQuadViewImpl cullFace(@Nullable Direction face) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.cullFace(this.data[this.baseIndex + HEADER_BITS], face);
        this.nominalFace(face);
        return this;
    }

    @Override
    public final MutableQuadViewImpl nominalFace(@Nullable Direction face) {
        this.nominalFace = face;
        return this;
    }

    @Override
    public final MutableQuadViewImpl material(RenderMaterial material) {
        if (material == null) {
            material = SodiumRenderer.STANDARD_MATERIAL;
        }

        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.material(this.data[this.baseIndex + HEADER_BITS], (RenderMaterialImpl) material);
        return this;
    }

    @Override
    public final MutableQuadViewImpl colorIndex(int colorIndex) {
        this.data[this.baseIndex + HEADER_COLOR_INDEX] = colorIndex;
        return this;
    }

    @Override
    public final MutableQuadViewImpl tag(int tag) {
        this.data[this.baseIndex + HEADER_TAG] = tag;
        return this;
    }

    @Override
    public MutableQuadViewImpl copyFrom(QuadView quad) {
        final QuadViewImpl q = (QuadViewImpl) quad;
        q.computeGeometry();

        System.arraycopy(q.data, q.baseIndex, this.data, this.baseIndex, EncodingFormat.TOTAL_STRIDE);
        this.faceNormal.set(q.faceNormal);
        this.nominalFace = q.nominalFace;
        this.isGeometryInvalid = false;

        if (quad instanceof MutableQuadViewImpl mutableQuad) {
            this.cachedSprite(mutableQuad.cachedSprite());
        } else {
            this.cachedSprite(null);
        }

        return this;
    }

    /**
     * Does the same work as {@link #fromVanilla(int[], int)}, but does not mark the geometry as invalid
     * and does not clear the cached sprite.
     * Only use this if you are also setting the geometry and sprite.
     */
    private void fromVanillaInternal(int[] quadData, int startIndex) {
        System.arraycopy(quadData, startIndex, this.data, this.baseIndex + HEADER_STRIDE, QuadView.VANILLA_QUAD_STRIDE);

        int colorIndex = this.baseIndex + VERTEX_COLOR;

        for (int i = 0; i < 4; i++) {
            this.data[colorIndex] = ColorHelper.fromVanillaColor(this.data[colorIndex]);
            colorIndex += VERTEX_STRIDE;
        }
    }

    @Override
    public final MutableQuadViewImpl fromVanilla(int[] quadData, int startIndex) {
        this.fromVanillaInternal(quadData, startIndex);
        this.isGeometryInvalid = true;
        this.cachedSprite(null);
        return this;
    }

    @Override
    public final MutableQuadViewImpl fromVanilla(BakedQuad quad, RenderMaterial material, @Nullable Direction cullFace) {
        this.fromVanillaInternal(quad.getVertices(), 0);
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.cullFace(0, cullFace);
        this.nominalFace(quad.getDirection());
        this.colorIndex(quad.getTintIndex());

        // TODO: Is this the same as hasShade?
        if (!((BakedQuadView) quad).hasShade()) {
            material = RenderMaterialImpl.setDisableDiffuse((RenderMaterialImpl) material, true);
        }

        if (material.ambientOcclusion().orElse(true) && !((BakedQuadView) quad).hasAO()) {
            material = RenderMaterialImpl.setAmbientOcclusion((RenderMaterialImpl) material, TriState.FALSE);
        }

        this.material(material);
        this.tag(0);

        // Copy geometry cached inside the quad
        BakedQuadView bakedView = (BakedQuadView) quad;
        NormI8.unpack(bakedView.getFaceNormal(), this.faceNormal);
        this.data[this.baseIndex + HEADER_FACE_NORMAL] = bakedView.getFaceNormal();
        int headerBits = EncodingFormat.lightFace(this.data[this.baseIndex + HEADER_BITS], bakedView.getLightFace());
        headerBits = EncodingFormat.normalFace(headerBits, bakedView.getNormalFace());
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.geometryFlags(headerBits, bakedView.getFlags());
        this.isGeometryInvalid = false;

        this.cachedSprite(quad.getSprite());
        return this;
    }

    /**
     * Emit the quad without clearing the underlying data.
     * Geometry is not guaranteed to be valid when called, but can be computed by calling {@link #computeGeometry()}.
     */
    public abstract void emitDirectly();

    @Override
    public final MutableQuadViewImpl emit() {
        this.emitDirectly();
        this.clear();
        return this;
    }
}
