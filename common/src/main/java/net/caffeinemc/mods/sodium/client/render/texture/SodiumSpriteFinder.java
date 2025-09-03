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

package net.caffeinemc.mods.sodium.client.render.texture;

import java.util.function.Predicate;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
public interface SodiumSpriteFinder {
    /**
     * Finds the atlas sprite containing the vertex centroid of the quad.
     * Vertex centroid is essentially the mean u,v coordinate - the intent being
     * to find a point that is unambiguously inside the sprite (vs on an edge.)
     *
     * <p>Should be reliable for any convex quad or triangle. May fail for non-convex quads.
     * Note that all the above refers to u,v coordinates. Geometric vertex does not matter,
     * except to the extent it was used to determine u,v.
     */
    TextureAtlasSprite find(ModelQuadView quad);

    /**
     * Alternative to {@link #find(ModelQuadView)} when vertex centroid is already
     * known or unsuitable.  Expects normalized (0-1) coordinates on the atlas texture,
     * which should already be the case for u,v values in vanilla baked quads and in
     * {@link QuadView} after calling {@link MutableQuadView#spriteBake(Sprite, int)}.
     *
     * <p>Coordinates must be in the sprite interior for reliable results. Generally will
     * be easier to use {@link #find(ModelQuadView)} unless you know the vertex
     * centroid will somehow not be in the quad interior. This method will be slightly
     * faster if you already have the centroid or another appropriate value.
     */
    TextureAtlasSprite find(float u, float v);
}