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

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;

/**
 * Collection of utilities for model implementations.
 */
public final class ModelHelper {
    /** @see #faceFromIndex(int) */
    private static final Direction[] FACES = Arrays.copyOf(Direction.values(), 7);

    /** Result from {@link #toFaceIndex(Direction)} for null values. */
    public static final int NULL_FACE_ID = 6;

    private ModelHelper() { }

    /**
     * Convenient way to encode faces that may be null.
     * Null is returned as {@link #NULL_FACE_ID}.
     * Use {@link #faceFromIndex(int)} to retrieve encoded face.
     */
    public static int toFaceIndex(@Nullable Direction face) {
        return face == null ? NULL_FACE_ID : face.get3DDataValue();
    }

    /**
     * Use to decode a result from {@link #toFaceIndex(Direction)}.
     * Return value will be null if encoded value was null.
     * Can also be used for no-allocation iteration of {@link Direction#values()},
     * optionally including the null face. (Use &lt; or  &lt;= {@link #NULL_FACE_ID}
     * to exclude or include the null value, respectively.)
     */
    @Nullable
    public static Direction faceFromIndex(int faceIndex) {
        return FACES[faceIndex];
    }
}
