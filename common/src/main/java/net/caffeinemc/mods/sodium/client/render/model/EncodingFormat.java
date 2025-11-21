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

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.helper.GeometryHelper;
import net.caffeinemc.mods.sodium.client.render.helper.ModelHelper;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

/**
 * Holds all the array offsets and bit-wise encoders/decoders for
 * packing/unpacking quad data in an array of integers.
 * All of this is implementation-specific - that's why it isn't a "helper" class.
 */
public final class EncodingFormat {
    private EncodingFormat() { }

    /** Count of integers in a conventional (un-modded) block or item vertex. */
    public static final int VANILLA_VERTEX_STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;

    /** Count of integers in a conventional (un-modded) block or item quad. */
    public static final int VANILLA_QUAD_STRIDE = VANILLA_VERTEX_STRIDE * 4;

    static final int HEADER_BITS = 0;
    static final int HEADER_FACE_NORMAL = 1;
    static final int HEADER_TINT_INDEX = 2;
    static final int HEADER_TAG = 3;
    public static final int HEADER_STRIDE = 4;

    static {
    }

    private static final int DIRECTION_COUNT = Direction.values().length;
    private static final int NULLABLE_DIRECTION_COUNT = DIRECTION_COUNT + 1;

    private static final @Nullable ChunkSectionLayer[] NULLABLE_BLOCK_RENDER_LAYERS = ArrayUtils.add(ChunkSectionLayer.values(), null);
    private static final int NULLABLE_BLOCK_RENDER_LAYER_COUNT = NULLABLE_BLOCK_RENDER_LAYERS.length;
    private static final TriState[] TRI_STATES = TriState.values();
    private static final int TRI_STATE_COUNT = TRI_STATES.length;
    private static final ItemStackRenderState.@Nullable FoilType[] NULLABLE_GLINTS = ArrayUtils.add(ItemStackRenderState.FoilType.values(), null);
    private static final int NULLABLE_GLINT_COUNT = NULLABLE_GLINTS.length;
    private static final SodiumShadeMode[] SHADE_MODES = SodiumShadeMode.values();
    private static final int SHADE_MODE_COUNT = SHADE_MODES.length;

    private static final int NULL_RENDER_LAYER_INDEX = NULLABLE_BLOCK_RENDER_LAYER_COUNT - 1;
    private static final int NULL_GLINT_INDEX = NULLABLE_GLINT_COUNT - 1;

    private static final int CULL_BIT_LENGTH = Mth.ceillog2(NULLABLE_DIRECTION_COUNT);
    private static final int LIGHT_BIT_LENGTH = Mth.ceillog2(DIRECTION_COUNT);
    private static final int NORMALS_BIT_LENGTH = 4;
    private static final int NORMAL_FACE_BIT_LENGTH = 3;
    private static final int GEOMETRY_BIT_LENGTH = GeometryHelper.FLAG_BIT_COUNT;
    private static final int RENDER_LAYER_BIT_LENGTH = Mth.ceillog2(NULLABLE_BLOCK_RENDER_LAYER_COUNT);
    private static final int EMISSIVE_BIT_LENGTH = 1;
    private static final int DIFFUSE_BIT_LENGTH = 1;
    private static final int AO_BIT_LENGTH = Mth.ceillog2(TRI_STATE_COUNT);
    private static final int GLINT_BIT_LENGTH = Mth.ceillog2(NULLABLE_GLINT_COUNT);
    private static final int SHADE_MODE_BIT_LENGTH = Mth.ceillog2(SHADE_MODE_COUNT);

    private static final int CULL_BIT_OFFSET = 0;
    private static final int LIGHT_BIT_OFFSET = CULL_BIT_OFFSET + CULL_BIT_LENGTH;
    private static final int NORMAL_FACE_BIT_OFFSET = LIGHT_BIT_OFFSET + LIGHT_BIT_LENGTH;
    private static final int NORMALS_BIT_OFFSET = NORMAL_FACE_BIT_OFFSET + NORMAL_FACE_BIT_LENGTH;
    private static final int GEOMETRY_BIT_OFFSET = NORMALS_BIT_OFFSET + NORMALS_BIT_LENGTH;
    private static final int RENDER_LAYER_BIT_OFFSET = GEOMETRY_BIT_OFFSET + GEOMETRY_BIT_LENGTH;
    private static final int EMISSIVE_BIT_OFFSET = RENDER_LAYER_BIT_OFFSET + RENDER_LAYER_BIT_LENGTH;
    private static final int DIFFUSE_BIT_OFFSET = EMISSIVE_BIT_OFFSET + EMISSIVE_BIT_LENGTH;
    private static final int AO_BIT_OFFSET = DIFFUSE_BIT_OFFSET + DIFFUSE_BIT_LENGTH;
    private static final int GLINT_BIT_OFFSET = AO_BIT_OFFSET + AO_BIT_LENGTH;
    private static final int SHADE_MODE_BIT_OFFSET = GLINT_BIT_OFFSET + GLINT_BIT_LENGTH;
    private static final int TOTAL_BIT_LENGTH = SHADE_MODE_BIT_OFFSET + SHADE_MODE_BIT_LENGTH;

    private static final int CULL_MASK = bitMask(CULL_BIT_LENGTH, CULL_BIT_OFFSET);
    private static final int LIGHT_MASK = bitMask(LIGHT_BIT_LENGTH, LIGHT_BIT_OFFSET);
    private static final int NORMAL_FACE_MASK = bitMask(NORMAL_FACE_BIT_LENGTH, NORMAL_FACE_BIT_OFFSET);
    private static final int NORMALS_MASK = bitMask(NORMALS_BIT_LENGTH, NORMALS_BIT_OFFSET);
    private static final int GEOMETRY_MASK = bitMask(GEOMETRY_BIT_LENGTH, GEOMETRY_BIT_OFFSET);
    private static final int RENDER_LAYER_MASK = bitMask(RENDER_LAYER_BIT_LENGTH, RENDER_LAYER_BIT_OFFSET);
    private static final int EMISSIVE_MASK = bitMask(EMISSIVE_BIT_LENGTH, EMISSIVE_BIT_OFFSET);
    private static final int DIFFUSE_MASK = bitMask(DIFFUSE_BIT_LENGTH, DIFFUSE_BIT_OFFSET);
    private static final int AO_MASK = bitMask(AO_BIT_LENGTH, AO_BIT_OFFSET);
    private static final int GLINT_MASK = bitMask(GLINT_BIT_LENGTH, GLINT_BIT_OFFSET);
    private static final int SHADE_MODE_MASK = bitMask(SHADE_MODE_BIT_LENGTH, SHADE_MODE_BIT_OFFSET);

    static {
    }

    private static int bitMask(int bitLength, int bitOffset) {
        return ((1 << bitLength) - 1) << bitOffset;
    }

    @Nullable
    static Direction cullFace(int bits) {
        return ModelHelper.faceFromIndex((bits & CULL_MASK) >>> CULL_BIT_OFFSET);
    }

    static int cullFace(int bits, @Nullable Direction face) {
        return (bits & ~CULL_MASK) | (ModelHelper.toFaceIndex(face) << CULL_BIT_OFFSET);
    }

    static Direction lightFace(int bits) {
        return ModelHelper.faceFromIndex((bits & LIGHT_MASK) >>> LIGHT_BIT_OFFSET);
    }

    static int lightFace(int bits, Direction face) {
        return (bits & ~LIGHT_MASK) | (ModelHelper.toFaceIndex(face) << LIGHT_BIT_OFFSET);
    }

    /** indicate if vertex normal has been set - bits correspond to vertex ordinals. */
    static int normalFlags(int bits) {
        return (bits & NORMALS_MASK) >>> NORMALS_BIT_OFFSET;
    }

    static int normalFlags(int bits, int normalFlags) {
        return (bits & ~NORMALS_MASK) | ((normalFlags << NORMALS_BIT_OFFSET) & NORMALS_MASK);
    }

    static int geometryFlags(int bits) {
        return (bits & GEOMETRY_MASK) >>> GEOMETRY_BIT_OFFSET;
    }

    static int geometryFlags(int bits, int geometryFlags) {
        return (bits & ~GEOMETRY_MASK) | ((geometryFlags << GEOMETRY_BIT_OFFSET) & GEOMETRY_MASK);
    }

    static ModelQuadFacing normalFace(int bits) {
        return ModelQuadFacing.VALUES[(bits & NORMAL_FACE_MASK) >>> NORMAL_FACE_BIT_OFFSET];
    }

    static int normalFace(int bits, ModelQuadFacing face) {
        return (bits & ~NORMAL_FACE_MASK) | ((face.ordinal() << NORMAL_FACE_BIT_OFFSET) & NORMAL_FACE_MASK);
    }

    @Nullable
    static ChunkSectionLayer renderLayer(int bits) {
        return NULLABLE_BLOCK_RENDER_LAYERS[(bits & RENDER_LAYER_MASK) >>> RENDER_LAYER_BIT_OFFSET];
    }

    static int renderLayer(int bits, @Nullable ChunkSectionLayer renderLayer) {
        int index = renderLayer == null ? NULL_RENDER_LAYER_INDEX : renderLayer.ordinal();
        return (bits & ~RENDER_LAYER_MASK) | (index << RENDER_LAYER_BIT_OFFSET);
    }

    static boolean emissive(int bits) {
        return (bits & EMISSIVE_MASK) != 0;
    }

    static int emissive(int bits, boolean emissive) {
        return emissive ? (bits | EMISSIVE_MASK) : (bits & ~EMISSIVE_MASK);
    }

    static boolean diffuseShade(int bits) {
        return (bits & DIFFUSE_MASK) != 0;
    }

    static int diffuseShade(int bits, boolean shade) {
        return shade ? (bits | DIFFUSE_MASK) : (bits & ~DIFFUSE_MASK);
    }

    static TriState ambientOcclusion(int bits) {
        return TRI_STATES[(bits & AO_MASK) >>> AO_BIT_OFFSET];
    }

    static int ambientOcclusion(int bits, TriState ao) {
        return (bits & ~AO_MASK) | (ao.ordinal() << AO_BIT_OFFSET);
    }

    static ItemStackRenderState.@Nullable FoilType glint(int bits) {
        return NULLABLE_GLINTS[(bits & GLINT_MASK) >>> GLINT_BIT_OFFSET];
    }

    static int glint(int bits, ItemStackRenderState.@Nullable FoilType glint) {
        int index = glint == null ? NULL_GLINT_INDEX : glint.ordinal();
        return (bits & ~GLINT_MASK) | (index << GLINT_BIT_OFFSET);
    }

    static SodiumShadeMode shadeMode(int bits) {
        return SHADE_MODES[(bits & SHADE_MODE_MASK) >>> SHADE_MODE_BIT_OFFSET];
    }

    static int shadeMode(int bits, SodiumShadeMode mode) {
        return (bits & ~SHADE_MODE_MASK) | (mode.ordinal() << SHADE_MODE_BIT_OFFSET);
    }
}
