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
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.TriState;
import org.jetbrains.annotations.Nullable;
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

    /** Size and where it comes from will vary in subtypes. But in all cases quad is fully encoded to array. */
    public int[] data;

    /** Beginning of the quad. Also, the header index. */
    public int baseIndex = 0;

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
            data[baseIndex + HEADER_FACE_NORMAL] = packedFaceNormal;

            // depends on face normal
            Direction lightFace = GeometryHelper.lightFace(this);
            data[baseIndex + HEADER_BITS] = EncodingFormat.lightFace(data[baseIndex + HEADER_BITS], lightFace);

            // depends on face normal
            data[baseIndex + HEADER_BITS] = EncodingFormat.normalFace(data[baseIndex + HEADER_BITS], ModelQuadFacing.fromPackedNormal(packedFaceNormal));

            // depends on light face
            data[baseIndex + HEADER_BITS] = EncodingFormat.geometryFlags(data[baseIndex + HEADER_BITS], ModelQuadFlags.getQuadFlags(this, lightFace));
        }
    }

    /** gets flags used for lighting - lazily computed via {@link ModelQuadFlags#getQuadFlags}. */
    public int geometryFlags() {
        computeGeometry();
        return EncodingFormat.geometryFlags(data[baseIndex + HEADER_BITS]);
    }

    public boolean hasShade() {
        return diffuseShade();
    }

    public Vector3f copyPos(int vertexIndex, @Nullable Vector3f target) {
        if (target == null) {
            target = new Vector3f();
        }

        final int index = baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X;
        target.set(Float.intBitsToFloat(data[index]), Float.intBitsToFloat(data[index + 1]), Float.intBitsToFloat(data[index + 2]));
        return target;
    }

    @Nullable
    public ChunkSectionLayer getRenderType() {
        return EncodingFormat.renderLayer(data[baseIndex + HEADER_BITS]);
    }

    public boolean emissive() {
        return EncodingFormat.emissive(data[baseIndex + HEADER_BITS]);
    }

    public boolean diffuseShade() {
        return EncodingFormat.diffuseShade(data[baseIndex + HEADER_BITS]);
    }

    public TriState ambientOcclusion() {
        return EncodingFormat.ambientOcclusion(data[baseIndex + HEADER_BITS]);
    }

    public ItemStackRenderState.@Nullable FoilType glint() {
        return EncodingFormat.glint(data[baseIndex + HEADER_BITS]);
    }

    public SodiumShadeMode getShadeMode() {
        return EncodingFormat.shadeMode(data[baseIndex + HEADER_BITS]);
    }

    public int baseColor(int vertexIndex) {
        return data[baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_COLOR];
    }

    public Vector2f copyUv(int vertexIndex, @Nullable Vector2f target) {
        if (target == null) {
            target = new Vector2f();
        }

        final int index = baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_U;
        target.set(Float.intBitsToFloat(data[index]), Float.intBitsToFloat(data[index + 1]));
        return target;
    }

    public int normalFlags() {
        return EncodingFormat.normalFlags(data[baseIndex + HEADER_BITS]);
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

    protected final int normalIndex(int vertexIndex) {
        return baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_NORMAL;
    }

    /**
     * This method will only return a meaningful value if {@link #hasNormal} returns {@code true} for the same vertex index.
     */
    public int packedNormal(int vertexIndex) {
        return data[normalIndex(vertexIndex)];
    }

    public float normalX(int vertexIndex) {
        return hasNormal(vertexIndex) ? NormI8.unpackX(data[normalIndex(vertexIndex)]) : Float.NaN;
    }

    public float normalY(int vertexIndex) {
        return hasNormal(vertexIndex) ? NormI8.unpackY(data[normalIndex(vertexIndex)]) : Float.NaN;
    }

    public float normalZ(int vertexIndex) {
        return hasNormal(vertexIndex) ? NormI8.unpackZ(data[normalIndex(vertexIndex)]) : Float.NaN;
    }

    @Nullable
    public Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
        if (hasNormal(vertexIndex)) {
            if (target == null) {
                target = new Vector3f();
            }

            final int normal = data[normalIndex(vertexIndex)];
            NormI8.unpack(normal, target);
            return target;
        } else {
            return null;
        }
    }

    @Nullable
    public final Direction getCullFace() {
        return EncodingFormat.cullFace(data[baseIndex + HEADER_BITS]);
    }

    public final ModelQuadFacing normalFace() {
        computeGeometry();
        return EncodingFormat.normalFace(data[baseIndex + HEADER_BITS]);
    }

    @Nullable
    public final Direction getNominalFace() {
        return nominalFace;
    }

    public final int packedFaceNormal() {
        computeGeometry();
        return data[baseIndex + HEADER_FACE_NORMAL];
    }

    public final Vector3f faceNormal() {
        computeGeometry();
        return faceNormal;
    }

    public final int getTag() {
        return data[baseIndex + HEADER_TAG];
    }

    public final void toVanilla(int[] target, int targetIndex) {
        System.arraycopy(data, baseIndex + HEADER_STRIDE, target, targetIndex, QUAD_STRIDE);

        // The color is the fourth integer in each vertex.
        // EncodingFormat.VERTEX_COLOR is not used because it also
        // contains the header size; vanilla quads do not have a header.
        int colorIndex = targetIndex + 3;

        for (int i = 0; i < 4; i++) {
            target[colorIndex] = ColorHelper.toVanillaColor(target[colorIndex]);
            colorIndex += EncodingFormat.VANILLA_VERTEX_STRIDE;
        }
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(data[baseIndex + idx * VERTEX_STRIDE + VERTEX_X]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(data[baseIndex + idx * VERTEX_STRIDE + VERTEX_Y]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(data[baseIndex + idx * VERTEX_STRIDE + VERTEX_Z]);
    }

    public float posByIndex(int vertexIndex, int coordinateIndex) { // TODO: check
        return Float.intBitsToFloat(data[baseIndex + vertexIndex * VERTEX_STRIDE + (VERTEX_X + coordinateIndex)]);
    }

    @Override
    public int getColor(int idx) {
        return ColorHelper.toVanillaColor(baseColor(idx));
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(data[baseIndex + idx * VERTEX_STRIDE + VERTEX_U]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(data[baseIndex + idx * VERTEX_STRIDE + VERTEX_V]);
    }

    @Override
    public int getVertexNormal(int idx) {
        return data[normalIndex(idx)];
    }

    @Override
    public int getFaceNormal() {
        return packedFaceNormal();
    }

    @Override
    public int getLight(int idx) {
        return data[baseIndex + idx * VERTEX_STRIDE + VERTEX_LIGHTMAP];
    }

    @Override
    public int getTintIndex() {
        return data[baseIndex + HEADER_TINT_INDEX];
    }

    @Override
    public TextureAtlasSprite getSprite() {
        throw new UnsupportedOperationException("Not available for QuadViewImpl.");
    }

    @Override
    public Direction getLightFace() {
        computeGeometry();
        return EncodingFormat.lightFace(data[baseIndex + HEADER_BITS]);
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
