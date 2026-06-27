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

import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.model.SodiumQuadAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
    private final SodiumQuadAtlas atlas;
    private int badSpriteCount = 0;

    public SodiumSpriteFinderImpl(Map<Identifier, TextureAtlasSprite> sprites, TextureAtlasSprite missingSprite, SodiumQuadAtlas atlas) {
        this.root = new Node(0.5f, 0.5f, 0.25f);
        this.missingSprite = missingSprite;
        this.atlas = atlas;
        sprites.values().forEach(this.root::add);
    }

    @Override
    public TextureAtlasSprite find(ModelQuadView quad) {
        float u = 0;
        float v = 0;

        for (int i = 0; i < 4; i++) {
            u += quad.getTexU(i);
            v += quad.getTexV(i);
        }

        return this.find(u * 0.25f, v * 0.25f);
    }

    @Override
    public TextureAtlasSprite find(float u, float v) {
        return this.root.find(u, v);
    }

    @Override
    public SodiumQuadAtlas getAtlas() {
        return this.atlas;
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
            this.cellRadius = radius;
        }

        static final float EPS = 0.00001f;

        void add(TextureAtlasSprite sprite) {
            if (sprite.getU0() < 0 - EPS || sprite.getU1() > 1 + EPS || sprite.getV0() < 0 - EPS || sprite.getV1() > 1 + EPS) {
                // Sprite has broken bounds. This SHOULD NOT happen, but in the past some mods have broken this.
                // Prefer failing with a log warning rather than risking a stack overflow.
                if (SodiumSpriteFinderImpl.this.badSpriteCount++ < 5) {
                    String errorMessage = "SpriteFinderImpl: Skipping sprite {} with broken bounds [{}, {}]x[{}, {}]. Sprite bounds should be between 0 and 1.";
                    LOGGER.error(errorMessage, sprite.contents().name(), sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1());
                }

                return;
            }

            final boolean lowU = sprite.getU0() < this.midU - EPS;
            final boolean highU = sprite.getU1() > this.midU + EPS;
            final boolean lowV = sprite.getV0() < this.midV - EPS;
            final boolean highV = sprite.getV1() > this.midV + EPS;

            if (lowU && lowV) {
                this.lowLow = this.addInner(sprite, this.lowLow, -1, -1);
            }

            if (lowU && highV) {
                this.lowHigh = this.addInner(sprite, this.lowHigh, -1, 1);
            }

            if (highU && lowV) {
                this.highLow = this.addInner(sprite, this.highLow, 1, -1);
            }

            if (highU && highV) {
                this.highHigh = this.addInner(sprite, this.highHigh, 1, 1);
            }
        }

        private Object addInner(TextureAtlasSprite sprite, @Nullable Object quadrant, int uStep, int vStep) {
            if (quadrant == null) {
                return sprite;
            } else if (quadrant instanceof Node node) {
                node.add(sprite);
                return quadrant;
            } else {
                Node n = new Node(this.midU + this.cellRadius * uStep, this.midV + this.cellRadius * vStep, this.cellRadius * 0.5f);

                if (quadrant instanceof TextureAtlasSprite prevSprite) {
                    n.add(prevSprite);
                }

                n.add(sprite);
                return n;
            }
        }

        private TextureAtlasSprite find(float u, float v) {
            if (u < this.midU) {
                return v < this.midV ? this.findInner(this.lowLow, u, v) : this.findInner(this.lowHigh, u, v);
            } else {
                return v < this.midV ? this.findInner(this.highLow, u, v) : this.findInner(this.highHigh, u, v);
            }
        }

        private TextureAtlasSprite findInner(@Nullable Object quadrant, float u, float v) {
            if (quadrant instanceof Node node) {
                return node.find(u, v);
            } else if (quadrant instanceof TextureAtlasSprite sprite) {
                return sprite;
            } else {
                return SodiumSpriteFinderImpl.this.missingSprite;
            }
        }
    }
}