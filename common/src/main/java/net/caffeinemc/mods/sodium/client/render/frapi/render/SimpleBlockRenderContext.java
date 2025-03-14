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
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;

public class SimpleBlockRenderContext extends AbstractBlockRenderContext {
    public static final ThreadLocal<SimpleBlockRenderContext> POOL = ThreadLocal.withInitial(SimpleBlockRenderContext::new);

    private final RandomSource random = RandomSource.createNewThreadLocalInstance();

    private MultiBufferSource vertexConsumers;
    private RenderType defaultRenderLayer;
    private float red;
    private float green;
    private float blue;
    private int light;

    @Nullable
    private RenderType lastRenderLayer;
    @Nullable
    private VertexConsumer lastVertexConsumer;
    private PoseStack.Pose matrices;
    private int overlay;

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final RenderMaterial mat = quad.material();
        final BlendMode blendMode = mat.blendMode();
        final RenderType renderLayer = blendMode == BlendMode.DEFAULT ? defaultRenderLayer : blendMode.blockRenderLayer;
        final VertexConsumer vertexConsumer;

        if (renderLayer == lastRenderLayer) {
            vertexConsumer = lastVertexConsumer;
        } else {
            lastVertexConsumer = vertexConsumer = vertexConsumers.getBuffer(renderLayer);
            lastRenderLayer = renderLayer;
        }

        VertexBufferWriter writer = VertexBufferWriter.of(vertexConsumer);

        if (quad.tintIndex() != -1) {
            final float red = this.red;
            final float green = this.green;
            final float blue = this.blue;

            for (int i = 0; i < 4; i++) {
                quad.color(i, ARGB.scaleRGB(quad.color(i), red, green, blue));
            }
        }

        if (mat.emissive()) {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightTexture.FULL_BRIGHT);
            }
        } else {
            final int light = this.light;

            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightTexture.lightCoordsWithEmission(quad.lightmap(i), light));
            }
        }

        bufferQuad(quad, writer);
    }

    private void bufferQuad(MutableQuadViewImpl quad, VertexBufferWriter writer) {
        // TODO: Confirm
        BakedModelEncoder.writeQuadVertices(writer, matrices, quad, 0xFFFFFFFF, light, overlay, false);

        if (quad.getSprite() != null) {
            SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
        }
    }

    public void bufferModel(PoseStack.Pose entry, MultiBufferSource vertexConsumers, BlockStateModel model, float red, float green, float blue, int light, int overlay, BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        matrices = entry;
        this.overlay = overlay;

        this.vertexConsumers = vertexConsumers;
        this.defaultRenderLayer = ItemBlockRenderTypes.getChunkRenderType(state);
        this.red = Mth.clamp(red, 0, 1);
        this.green = Mth.clamp(green, 0, 1);
        this.blue = Mth.clamp(blue, 0, 1);
        this.light = light;

        random.setSeed(42L);

        ((FabricBlockStateModel) model).emitQuads(getEmitter(), blockView, pos, state, random, cullFace -> false);

        this.vertexConsumers = null;
        lastRenderLayer = null;
        lastVertexConsumer = null;
    }
}