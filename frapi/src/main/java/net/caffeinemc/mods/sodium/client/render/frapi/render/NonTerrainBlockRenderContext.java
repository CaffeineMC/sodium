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

package net.caffeinemc.mods.sodium.client.render.frapi.render;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.SingleBlockLightDataCache;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.MutableQuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.client.renderer.v1.render.AltModelBlockRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockTintsFactory;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class NonTerrainBlockRenderContext extends AbstractBlockRenderContext implements AltModelBlockRenderer, QuadTransform {
    private final boolean allowAO;
    private final BlockColors colorMap;
    private final SingleBlockLightDataCache lightDataCache = new SingleBlockLightDataCache();

    private boolean defaultAo;
    private final Vector3f offset = new Vector3f();
    private int tintCacheIndex = -1;
    private int tintCacheValue;
    private boolean tintSourcesInitialized;
    private final List<@Nullable BlockTintSource> tintSources = new ObjectArrayList<>();
    private final IntList computedTintValues = new IntArrayList();

    public NonTerrainBlockRenderContext(boolean ambientOcclusion, boolean cull, BlockColors blockColors) {
        this.allowAO = ambientOcclusion;
        this.enableCulling = cull;
        this.colorMap = blockColors;
        this.lighters = new LightPipelineProvider(this.lightDataCache);
        this.random = new SingleThreadedRandomSource(42L);
    }

    private void configureTintCache(final BlockState blockState, BlockAndTintGetter level, BlockPos pos) {
        List<BlockTintSource> tintSources = this.colorMap.getTintSources(blockState);
        int tintSourceCount = tintSources.size();

        if (tintSourceCount > 0) {
            this.tintSources.addAll(tintSources);

            for (int i = 0; i < tintSourceCount; ++i) {
                this.computedTintValues.add(-1);
            }
        } else {
            final BlockTintsFactory factory = BlockColorRegistry.getFactory(blockState);

            if (factory != null) {
                factory.collect(blockState, level, pos, this.computedTintValues);
            }

            if (!this.computedTintValues.isEmpty()) {
                for (int i = 0; i < this.computedTintValues.size(); i++) {
                    this.tintSources.add(null);
                }
            }
        }
    }

    private int computeTintColor(final BlockAndTintGetter level, final BlockState state, final BlockPos pos, final int tintIndex) {
        if (!this.tintSourcesInitialized) {
            this.configureTintCache(state, level, pos);
            this.tintSourcesInitialized = true;
        }

        if (tintIndex >= this.tintSources.size()) {
            return -1;
        } else {
            BlockTintSource tintSource = this.tintSources.set(tintIndex, null);

            if (tintSource != null) {
                int computedTintValue = tintSource.colorInWorld(state, level, pos);
                this.computedTintValues.set(tintIndex, computedTintValue);
                return computedTintValue;
            } else {
                return this.computedTintValues.getInt(tintIndex);
            }
        }
    }

    private int getTintColor(final BlockAndTintGetter level, final BlockState state, final BlockPos pos, final int tintIndex) {
        if (this.tintCacheIndex == tintIndex) {
            return this.tintCacheValue;
        } else {
            int tintColor = this.computeTintColor(level, state, pos, tintIndex);
            this.tintCacheIndex = tintIndex;
            this.tintCacheValue = tintColor;
            return tintColor;
        }
    }

    private void resetTintCache() {
        this.tintCacheIndex = -1;

        if (this.tintSourcesInitialized) {
            this.tintSources.clear();
            this.computedTintValues.clear();
            this.tintSourcesInitialized = false;
        }
    }

    @Override
    public void tesselateBlock(QuadEmitter output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos, BlockState blockState, BlockStateModel model, long seed) {
        this.level = level;
        this.state = blockState;
        this.pos = pos;

        Vec3 offset = blockState.getOffset(pos);
        this.offset.set(x + offset.x, y + offset.y, z + offset.z);
        this.defaultAo = this.allowAO && blockState.getLightEmission() == 0;

        this.lightDataCache.reset(pos, level);
        this.prepareCulling(this.enableCulling);

        this.random.setSeed(seed);

        output.clear();
        output.pushTransform(this);

        model.emitQuads(output, level, pos, this.state, this.random, this::isFaceCulled);

        this.level = null;
        output.popTransform();
        this.lightDataCache.release();
        this.resetTintCache();
    }

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final TriState aoMode = quad.ambientOcclusion();
        final SodiumShadeMode shadeMode = quad.getShadeMode();
        final LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode != TriState.FALSE && this.defaultAo ? LightMode.SMOOTH : LightMode.FLAT;
        }
        final boolean emissive = quad.emissive();

        this.tintQuad(quad);
        this.shadeQuad(quad, lightMode, emissive, shadeMode);
        quad.translate(this.offset.x, this.offset.y, this.offset.z);
        var sprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
        if (sprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }

    private void tintQuad(MutableQuadViewImpl quad) {
        int tintIndex = quad.getTintIndex();

        if (tintIndex != -1) {
            int tint = this.getTintColor(this.level, this.state, this.pos, tintIndex);

            for (int i = 0; i < 4; i++) {
                quad.setColor(i, ColorMixer.mulComponentWise(tint, quad.baseColor(i)));
            }
        }
    }

    @Override
    protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, SodiumShadeMode shadeMode) {
        super.shadeQuad(quad, lightMode, emissive, shadeMode);

        float[] brightnesses = this.quadLightData.br;

        for (int i = 0; i < 4; i++) {
            quad.setColor(i, ColorARGB.mulRGB(quad.baseColor(i), brightnesses[i]));
        }
    }

    @Override
    public boolean transform(MutableQuadView mutableQuadView) {
        this.renderQuad(((MutableQuadViewWrapper) mutableQuadView).getOriginal());
        return true;
    }
}
