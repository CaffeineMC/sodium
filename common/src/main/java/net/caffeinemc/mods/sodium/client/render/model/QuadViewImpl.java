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
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.caffeinemc.mods.sodium.client.render.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.helper.GeometryHelper;
import net.caffeinemc.mods.sodium.client.render.helper.NormalHelper;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import org.jspecify.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static net.caffeinemc.mods.sodium.client.render.model.EncodingFormat.*;

/**
 * Base class for all quads / quad makers. Handles the ugly bits
 * of maintaining and encoding the quad state.
 */
public class QuadViewImpl implements ModelQuadView {
    @Nullable
    protected Direction nominalFace;

    /** True when face normal, light face, normal face, or geometry flags may not match geometry. */
    protected boolean isGeometryInvalid = true;
    protected final Vector3f faceNormal = new Vector3f();
    protected int packedNormal = NormI8.pack(0, 1, 0);
    protected int normalFlags;

    protected int header = 0;
    protected int tintIndex = 0;
    protected final Vector3f[] positions = new Vector3f[] {
            new Vector3f(),
            new Vector3f(),
            new Vector3f(),
            new Vector3f()
    };

    protected long[] uv = new long[4];
    protected int[] light = new int[4];
    protected int[] colors = new int[4];

    protected int maxLightEmission;
    protected int tag;

    /** Size and where it comes from will vary in subtypes. But in all cases quad is fully encoded to array. */

    /** Beginning of the quad. Also, the header index. */

    /**
     * Decodes necessary state from the backing data array.
     * The encoded data must contain valid computed geometry.
     */
    public void load() {
        isGeometryInvalid = false;
        nominalFace = getLightFace();
        NormI8.unpack(packedFaceNormal(), faceNormal);
    }

    protected void computeGeometry() {
        if (isGeometryInvalid) {
            isGeometryInvalid = false;

            NormalHelper.computeFaceNormal(faceNormal, this);
            int packedFaceNormal = NormI8.pack(faceNormal);
            packedNormal = packedFaceNormal;

            // depends on face normal
            Direction lightFace = GeometryHelper.lightFace(this);
            header = EncodingFormat.lightFace(header, lightFace);

            // depends on face normal
            header = EncodingFormat.normalFace(header, ModelQuadFacing.fromPackedNormal(packedFaceNormal));

            // depends on light face
            header = EncodingFormat.geometryFlags(header, ModelQuadFlags.getQuadFlags(this, lightFace));
        }
    }

    /** gets flags used for lighting - lazily computed via {@link ModelQuadFlags#getQuadFlags}. */
    public int geometryFlags() {
        computeGeometry();
        return EncodingFormat.geometryFlags(header);
    }

    public boolean hasShade() {
        return diffuseShade();
    }

    public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
        if (target == null) {
            target = new Vector3f();
        }

        target.set(positions[vertexIndex].x, positions[vertexIndex].y, positions[vertexIndex].z);
        return target;
    }

    @Nullable
    public ChunkSectionLayer getRenderType() {
        return EncodingFormat.renderLayer(header);
    }

    public boolean emissive() {
        return EncodingFormat.emissive(header);
    }

    public boolean diffuseShade() {
        return EncodingFormat.diffuseShade(header);
    }

    public TriState ambientOcclusion() {
        return EncodingFormat.ambientOcclusion(header);
    }

    public ItemStackRenderState.@Nullable FoilType glint() {
        return EncodingFormat.glint(header);
    }

    public SodiumShadeMode getShadeMode() {
        return EncodingFormat.shadeMode(header);
    }

    public int baseColor(int vertexIndex) {
        return colors[vertexIndex];
    }

    public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
        if (target == null) {
            target = new Vector2f();
        }

        target.set(UVPair.unpackU(uv[vertexIndex]), UVPair.unpackV(uv[vertexIndex]));
        return target;
    }

    public int normalFlags() {
        return EncodingFormat.normalFlags(header);
    }

    public boolean hasNormal(int vertexIndex) {
        return (normalFlags() & (1 << vertexIndex)) != 0;
    }

    /** True if any vertex normal has been set. */
    public boolean hasVertexNormals() {
        return normalFlags() != 0;
    }

    /** True if all vertex normals have been set. */
    public boolean hasAllVertexNormals() {
        return (normalFlags() & 0b1111) == 0b1111;
    }

    /**
     * This method will only return a meaningful value if {@link #hasNormal} returns {@code true} for the same vertex index.
     */
    public int packedNormal(int vertexIndex) {
        return packedNormal;
    }

    public float normalX(int vertexIndex) {
        throw new IllegalStateException("Unimplemented as of 1.21.11"); // return hasNormal(vertexIndex) ? NormI8.unpackX(data[normalIndex(vertexIndex)]) : Float.NaN;
    }

    public float normalY(int vertexIndex) {
        throw new IllegalStateException("Unimplemented as of 1.21.11"); // return hasNormal(vertexIndex) ? NormI8.unpackX(data[normalIndex(vertexIndex)]) : Float.NaN;
    }

    public float normalZ(int vertexIndex) {
        throw new IllegalStateException("Unimplemented as of 1.21.11"); // return hasNormal(vertexIndex) ? NormI8.unpackX(data[normalIndex(vertexIndex)]) : Float.NaN;
    }

    @Nullable
    public Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
        if (hasNormal(vertexIndex)) {
            if (target == null) {
                target = new Vector3f();
            }

            final int normal = packedNormal;
            NormI8.unpack(normal, target);
            return target;
        } else {
            return null;
        }
    }

    @Nullable
    public final Direction getCullFace() {
        return EncodingFormat.cullFace(header);
    }

    public final ModelQuadFacing normalFace() {
        computeGeometry();
        return EncodingFormat.normalFace(header);
    }

    @Nullable
    public final Direction getNominalFace() {
        return nominalFace;
    }

    public final int packedFaceNormal() {
        computeGeometry();
        return packedNormal;
    }

    public final Vector3f faceNormal() {
        computeGeometry();
        return faceNormal;
    }

    public final int getTag() {
        return tag;
    }

    @Override
    public float getX(int idx) {
        return positions[idx].x;
    }

    @Override
    public float getY(int idx) {
        return positions[idx].y;
    }

    @Override
    public float getZ(int idx) {
        return positions[idx].z;
    }

    public float posByIndex(int vertexIndex, int coordinateIndex) { // TODO: check
        return positions[vertexIndex].get(coordinateIndex);
    }

    @Override
    public int getColor(int idx) {
        return ColorHelper.toVanillaColor(baseColor(idx));
    }

    @Override
    public float getTexU(int idx) {
        return UVPair.unpackU(uv[idx]);
    }

    @Override
    public float getTexV(int idx) {
        return UVPair.unpackV(uv[idx]);
    }

    @Override
    public int getVertexNormal(int idx) {
        return 0; // data[normalIndex(idx)];
    }

    @Override
    public int getFaceNormal() {
        return packedFaceNormal();
    }

    @Override
    public int getLight(int idx) {
        return light[idx];
    }

    @Override
    public int getTintIndex() {
        return tintIndex;
    }

    @Override
    public TextureAtlasSprite getSprite() {
        throw new UnsupportedOperationException("Not available for QuadViewImpl.");
    }

    @Override
    public Direction getLightFace() {
        computeGeometry();
        return EncodingFormat.lightFace(header);
    }

    @Override
    public int getMaxLightQuad(int idx) {
        return getLight(idx);
    }

    @Override
    public int getFlags() {
        return geometryFlags();
    }
}
