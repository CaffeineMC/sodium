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

import java.util.Map;

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indexes an atlas sprite to allow fast lookup of Sprites from
 * baked vertex coordinates.  Implementation is a straightforward
 * quad tree. Other options that were considered were linear search
 * (slow) and direct indexing of fixed-size cells. Direct indexing
 * would be fastest but would be memory-intensive for large atlases
 * and unsuitable for any atlas that isn't consistently aligned to
 * a fixed cell size.
 */
public class SodiumSpriteFinderImpl implements SodiumSpriteFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SodiumSpriteFinderImpl.class);

    private final Node root;
    private final TextureAtlasSprite missingSprite;
    private int badSpriteCount = 0;

    public SodiumSpriteFinderImpl(Map<ResourceLocation, TextureAtlasSprite> sprites, TextureAtlasSprite missingSprite) {
        root = new Node(0.5f, 0.5f, 0.25f);
        this.missingSprite = missingSprite;
        sprites.values().forEach(root::add);
    }

    @Override
    public TextureAtlasSprite find(ModelQuadView quad) {
        float u = 0;
        float v = 0;

        for (int i = 0; i < 4; i++) {
            u += quad.getTexU(i);
            v += quad.getTexV(i);
        }

        return find(u * 0.25f, v * 0.25f);
    }

    @Override
    public TextureAtlasSprite find(float u, float v) {
        return root.find(u, v);
    }

    private class Node {
        final float midU;
        final float midV;
        final float cellRadius;

        @Nullable
        Object lowLow = null;
        @Nullable
        Object lowHigh = null;
        @Nullable
        Object highLow = null;
        @Nullable
        Object highHigh = null;

        Node(float midU, float midV, float radius) {
            this.midU = midU;
            this.midV = midV;
            cellRadius = radius;
        }

        static final float EPS = 0.00001f;

        void add(TextureAtlasSprite sprite) {
            if (sprite.getU0() < 0 - EPS || sprite.getU1() > 1 + EPS || sprite.getV0() < 0 - EPS || sprite.getV1() > 1 + EPS) {
                // Sprite has broken bounds. This SHOULD NOT happen, but in the past some mods have broken this.
                // Prefer failing with a log warning rather than risking a stack overflow.
                if (badSpriteCount++ < 5) {
                    String errorMessage = "SpriteFinderImpl: Skipping sprite {} with broken bounds [{}, {}]x[{}, {}]. Sprite bounds should be between 0 and 1.";
                    LOGGER.error(errorMessage, sprite.contents().name(), sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
                }

                return;
            }

            final boolean lowU = sprite.getU0() < midU - EPS;
            final boolean highU = sprite.getU1() > midU + EPS;
            final boolean lowV = sprite.getV0() < midV - EPS;
            final boolean highV = sprite.getV1() > midV + EPS;

            if (lowU && lowV) {
                lowLow = addInner(sprite, lowLow, -1, -1);
            }

            if (lowU && highV) {
                lowHigh = addInner(sprite, lowHigh, -1, 1);
            }

            if (highU && lowV) {
                highLow = addInner(sprite, highLow, 1, -1);
            }

            if (highU && highV) {
                highHigh = addInner(sprite, highHigh, 1, 1);
            }
        }

        private Object addInner(TextureAtlasSprite sprite, @Nullable Object quadrant, int uStep, int vStep) {
            if (quadrant == null) {
                return sprite;
            } else if (quadrant instanceof Node node) {
                node.add(sprite);
                return quadrant;
            } else {
                Node n = new Node(midU + cellRadius * uStep, midV + cellRadius * vStep, cellRadius * 0.5f);

                if (quadrant instanceof TextureAtlasSprite prevSprite) {
                    n.add(prevSprite);
                }

                n.add(sprite);
                return n;
            }
        }

        private TextureAtlasSprite find(float u, float v) {
            if (u < midU) {
                return v < midV ? findInner(lowLow, u, v) : findInner(lowHigh, u, v);
            } else {
                return v < midV ? findInner(highLow, u, v) : findInner(highHigh, u, v);
            }
        }

        private TextureAtlasSprite findInner(@Nullable Object quadrant, float u, float v) {
            if (quadrant instanceof Node node) {
                return node.find(u, v);
            } else if (quadrant instanceof TextureAtlasSprite sprite) {
                return sprite;
            } else {
                return missingSprite;
            }
        }
    }
}