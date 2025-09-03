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

package net.caffeinemc.mods.sodium.client.render.helper;

import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Handles most texture-baking use cases for model loaders and model libraries
 * via {@link #bakeSprite(MutableQuadViewImpl, TextureAtlasSprite, int)}. Also used by the API
 * itself to implement automatic block-breaking models for enhanced models.
 */
public class TextureHelper {
    private TextureHelper() { }

    private static final float NORMALIZER = 1f / 16f;
    
    /**
     * When enabled, causes texture to appear with no rotation. This is the default and does not have to be specified
     * explicitly. Can be overridden by other rotation flags.
     */
    public static final int BAKE_ROTATE_NONE = 0;

    /**
     * When enabled, causes texture to appear rotated 90 degrees clockwise.
     */
    public static final int BAKE_ROTATE_90 = 1;

    /**
     * When enabled, causes texture to appear rotated 180 degrees.
     */
    public static final int BAKE_ROTATE_180 = 2;

    /**
     * When enabled, causes texture to appear rotated 270 degrees clockwise.
     */
    public static final int BAKE_ROTATE_270 = 3;

    public static final int BAKE_LOCK_UV = 4;

    public static final int BAKE_FLIP_U = 8;

    /**
     * Same as {@link #BAKE_FLIP_U} but for V coordinate.
     */
    public static final int BAKE_FLIP_V = 16;

    /**
     * UV coordinates by default are assumed to be 0-16 scale for consistency
     * with conventional Minecraft model format. This is scaled to 0-1 during
     * baking before interpolation. Model loaders that already have 0-1 coordinates
     * can avoid wasteful multiplication/division by passing 0-1 coordinates directly.
     */
    public static final int BAKE_NORMALIZED = 32;
    
    /**
     * Bakes textures in the provided vertex data, handling UV locking,
     * rotation, interpolation, etc. Textures must not be already baked.
     */
    public static void bakeSprite(MutableQuadViewImpl quad, TextureAtlasSprite sprite, int bakeFlags) {
        if (quad.getNominalFace() != null && (BAKE_LOCK_UV & bakeFlags) != 0) {
            // Assigns normalized UV coordinates based on vertex positions
            applyModifier(quad, UVLOCKERS[quad.getNominalFace().get3DDataValue()]);
        } else if ((BAKE_NORMALIZED & bakeFlags) == 0) { // flag is NOT set, UVs are assumed to not be normalized yet as is the default, normalize through dividing by 16
            // Scales from 0-16 to 0-1
            applyModifier(quad, (q, i) -> q.setUV(i, q.getTexU(i) * NORMALIZER, q.getTexV(i) * NORMALIZER));
        }

        final int rotation = bakeFlags & 3;

        if (rotation != 0) {
            // Rotates texture around the center of sprite.
            // Assumes normalized coordinates.
            applyModifier(quad, ROTATIONS[rotation]);
        }

        if ((BAKE_FLIP_U & bakeFlags) != 0) {
            // Inverts U coordinates.  Assumes normalized (0-1) values.
            applyModifier(quad, (q, i) -> q.setUV(i, 1 - q.getTexU(i), q.getTexV(i)));
        }

        if ((BAKE_FLIP_V & bakeFlags) != 0) {
            // Inverts V coordinates.  Assumes normalized (0-1) values.
            applyModifier(quad, (q, i) -> q.setUV(i, q.getTexU(i), 1 - q.getTexV(i)));
        }

        interpolate(quad, sprite);
    }

    /**
     * Faster than sprite method. Sprite computes span and normalizes inputs each call,
     * so we'd have to denormalize before we called, only to have the sprite renormalize immediately.
     */
    private static void interpolate(MutableQuadViewImpl q, TextureAtlasSprite sprite) {
        final float uMin = sprite.getU0();
        final float uSpan = sprite.getU1() - uMin;
        final float vMin = sprite.getV0();
        final float vSpan = sprite.getV1() - vMin;

        for (int i = 0; i < 4; i++) {
            q.setUV(i, uMin + q.getTexU(i) * uSpan, vMin + q.getTexV(i) * vSpan);
        }
    }

    @FunctionalInterface
    private interface VertexModifier {
        void apply(MutableQuadViewImpl quad, int vertexIndex);
    }

    private static void applyModifier(MutableQuadViewImpl quad, VertexModifier modifier) {
        for (int i = 0; i < 4; i++) {
            modifier.apply(quad, i);
        }
    }

    private static final VertexModifier[] ROTATIONS = new VertexModifier[] {
            null,
            (q, i) -> q.setUV(i, q.getTexV(i), 1 - q.getTexU(i)), //90
            (q, i) -> q.setUV(i, 1 - q.getTexU(i), 1 - q.getTexV(i)), //180
            (q, i) -> q.setUV(i, 1 - q.getTexV(i), q.getTexU(i)) // 270
    };

    private static final VertexModifier[] UVLOCKERS = new VertexModifier[6];

    static {
        UVLOCKERS[Direction.EAST.get3DDataValue()] = (q, i) -> q.setUV(i, 1 - q.getZ(i), 1 - q.getY(i));
        UVLOCKERS[Direction.WEST.get3DDataValue()] = (q, i) -> q.setUV(i, q.getZ(i), 1 - q.getY(i));
        UVLOCKERS[Direction.NORTH.get3DDataValue()] = (q, i) -> q.setUV(i, 1 - q.getX(i), 1 - q.getY(i));
        UVLOCKERS[Direction.SOUTH.get3DDataValue()] = (q, i) -> q.setUV(i, q.getX(i), 1 - q.getY(i));
        UVLOCKERS[Direction.DOWN.get3DDataValue()] = (q, i) -> q.setUV(i, q.getX(i), 1 - q.getZ(i));
        UVLOCKERS[Direction.UP.get3DDataValue()] = (q, i) -> q.setUV(i, q.getX(i), q.getZ(i));
    }
}
