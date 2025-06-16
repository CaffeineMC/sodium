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

package net.caffeinemc.mods.sodium.mixin.features.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;

@Mixin(FallingBlockRenderer.class)
abstract class FallingBlockRendererMixin extends EntityRenderer<FallingBlockEntity, FallingBlockRenderState> {
    @Shadow
    @Final
    private BlockRenderDispatcher dispatcher;

    protected FallingBlockRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    /**
     * @author IMS
     * @reason Support multi-render layer models. This is taken from Indigo.
     */
    @Overwrite
    public void render(FallingBlockRenderState renderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int light) {
        BlockState blockState = renderState.blockState;

        if (blockState.getRenderShape() == RenderShape.MODEL) {
            poseStack.pushPose();
            poseStack.translate(-0.5, 0.0, -0.5);

            BlockStateModel model = dispatcher.getBlockModel(blockState);
            long seed = blockState.getSeed(renderState.startBlockPos);
            (((FabricBlockModelRenderer) dispatcher.getModelRenderer())).render(renderState, model, blockState, renderState.blockPos, poseStack, RenderLayerHelper.movingDelegate(multiBufferSource), false, seed, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
            super.render(renderState, poseStack, multiBufferSource, light);
        }
    }
}
