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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.SingleBlockLightDataCache;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class NonTerrainBlockRenderContext extends AbstractBlockRenderContext {
    public static final ThreadLocal<NonTerrainBlockRenderContext> POOL = ThreadLocal.withInitial(NonTerrainBlockRenderContext::new);

    private BlockColors colorMap;
    private final SingleBlockLightDataCache lightDataCache = new SingleBlockLightDataCache();

    private BlockVertexConsumerProvider vertexConsumer;
    private Matrix4f matPosition;
    private boolean trustedNormals;
    private Matrix3f matNormal;
    private int overlay;

    public NonTerrainBlockRenderContext() {
        this.lighters = new LightPipelineProvider(this.lightDataCache);
        this.random = new SingleThreadedRandomSource(42L);
    }

    public void renderModel(BlockAndTintGetter blockView, BlockColors blockColors, BlockStateModel model, BlockState state, BlockPos pos, PoseStack poseStack, BlockVertexConsumerProvider buffer, boolean cull, long seed, int overlay) {
        this.level = blockView;
        this.state = state;
        this.pos = pos;
        this.colorMap = blockColors;


        this.vertexConsumer = buffer;
        this.matPosition = poseStack.last().pose();
        this.trustedNormals = poseStack.last().trustedNormals;
        this.matNormal = poseStack.last().normal();
        this.overlay = overlay;
        this.defaultRenderType = ItemBlockRenderTypes.getChunkRenderType(state);

        this.lightDataCache.reset(pos, blockView);
        this.prepareCulling(cull);

        random.setSeed(seed);
        ((FabricBlockStateModel) model).emitQuads(getEmitter(), blockView, pos, state, this.random, this::isFaceCulled);

        this.defaultRenderType = null;
        this.level = null;
        this.lightDataCache.release();
        this.vertexConsumer = null;
    }

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final TriState aoMode = quad.ambientOcclusion();
        final ShadeMode shadeMode = quad.shadeMode();
        final LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode.get() ? LightMode.SMOOTH : LightMode.FLAT;
        }
        final boolean emissive = quad.emissive();

        VertexConsumer vertexConsumer = getVertexConsumer(quad.renderLayer());

        tintQuad(quad);
        shadeQuad(quad, lightMode, emissive, shadeMode);
        bufferQuad(quad, vertexConsumer);
    }

    private VertexConsumer getVertexConsumer(ChunkSectionLayer blendMode) {
        return vertexConsumer.getBuffer(blendMode == null ? defaultRenderType : blendMode);
    }

    private RenderType toRenderLayer(ChunkSectionLayer defaultRenderType) {
        return switch (defaultRenderType) {
            case SOLID -> RenderType.solid();
            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucentMovingBlock();
            case TRIPWIRE -> RenderType.tripwire();
        };
    }

    private void tintQuad(MutableQuadViewImpl quad) {
        if (quad.tintIndex() != -1) {
            final int blockColor = 0xFF000000 | this.colorMap.getColor(this.state, this.level, this.pos, quad.tintIndex());

            for (int i = 0; i < 4; i++) {
                quad.color(i, ColorMixer.mulComponentWise(blockColor, quad.color(i)));
            }
        }
    }

    @Override
    protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode) {
        super.shadeQuad(quad, lightMode, emissive, shadeMode);

        float[] brightnesses = this.quadLightData.br;

        for (int i = 0; i < 4; i++) {
            quad.color(i, ColorARGB.mulRGB(quad.color(i), brightnesses[i]));
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
        QuadEncoder.writeQuadVertices(quad, vertexConsumer, overlay, matPosition, trustedNormals, matNormal);
        var sprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
        if (sprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }
}
