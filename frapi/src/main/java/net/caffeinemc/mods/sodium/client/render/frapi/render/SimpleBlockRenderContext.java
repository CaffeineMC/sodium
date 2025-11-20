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
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.QuadEncoder;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.render.BlockVertexConsumerProvider;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SimpleBlockRenderContext extends AbstractBlockRenderContext {
    public static final ThreadLocal<SimpleBlockRenderContext> POOL = ThreadLocal.withInitial(SimpleBlockRenderContext::new);

    private final RandomSource random = RandomSource.createNewThreadLocalInstance();

    private BlockVertexConsumerProvider vertexConsumers;
    private float red;
    private float green;
    private float blue;
    private int light;

    @Nullable
    private ChunkSectionLayer lastRenderLayer;
    @Nullable
    private VertexConsumer lastVertexConsumer;
    private PoseStack.Pose matrices;
    private int overlay;

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final ChunkSectionLayer quadRenderLayer = quad.getRenderType();
        final ChunkSectionLayer renderLayer = quadRenderLayer == null ? defaultRenderType : quadRenderLayer;
        final VertexConsumer vertexConsumer;

        if (renderLayer == lastRenderLayer) {
            vertexConsumer = lastVertexConsumer;
        } else {
            lastVertexConsumer = vertexConsumer = vertexConsumers.getBuffer(renderLayer);
            lastRenderLayer = renderLayer;
        }

        if (quad.getTintIndex() != -1) {
            final float red = this.red;
            final float green = this.green;
            final float blue = this.blue;

            for (int i = 0; i < 4; i++) {
                quad.setColor(i, ARGB.scaleRGB(quad.baseColor(i), red, green, blue));
            }
        }

        if (quad.emissive()) {
            for (int i = 0; i < 4; i++) {
                quad.setLight(i, LightTexture.FULL_BRIGHT);
            }
        } else {
            final int light = this.light;

            for (int i = 0; i < 4; i++) {
                quad.setLight(i, ColorHelper.maxBrightness(quad.getLight(i), light));
            }
        }

        QuadEncoder.writeQuadVertices(quad, vertexConsumer, overlay, matrices.pose(), matrices.trustedNormals, matrices.normal());

        SpriteUtil.INSTANCE.markSpriteActive(quad.sprite(SpriteFinderCache.forBlockAtlas()));
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

    public void bufferModel(PoseStack.Pose entry, BlockVertexConsumerProvider vertexConsumers, BlockStateModel model, float red, float green, float blue, int light, int overlay, BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        matrices = entry;
        this.overlay = overlay;

        this.prepareAoInfo(true);

        this.vertexConsumers = vertexConsumers;
        this.defaultRenderType = ItemBlockRenderTypes.getChunkRenderType(state);
        this.red = Mth.clamp(red, 0, 1);
        this.green = Mth.clamp(green, 0, 1);
        this.blue = Mth.clamp(blue, 0, 1);
        this.light = light;
        this.level = blockView;
        this.state = state;
        this.pos = pos;

        random.setSeed(42L);

        ((FabricBlockStateModel) model).emitQuads(((ExtendedMutableQuadViewImpl) getForEmitting()).getWrapper(), blockView, pos, state, random, cullFace -> false);

        this.level = null;
        this.state = null;
        this.pos = null;
        this.vertexConsumers = null;
        lastRenderLayer = null;
        lastVertexConsumer = null;
    }
}