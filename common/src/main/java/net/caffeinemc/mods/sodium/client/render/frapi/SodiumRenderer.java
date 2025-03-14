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

package net.caffeinemc.mods.sodium.client.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.material.MaterialFinderImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.material.RenderMaterialImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableMeshImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.frapi.render.SimpleBlockRenderContext;
import net.caffeinemc.mods.sodium.mixin.features.render.frapi.BlockRenderDispatcherAccessor;
import net.caffeinemc.mods.sodium.mixin.features.render.frapi.ModelBlockRendererAccessor;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.render.FabricBlockModelRenderer;
import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

import java.util.HashMap;
import java.util.List;

/**
 * The Sodium renderer implementation.
 */
public class SodiumRenderer implements Renderer {
    public static final SodiumRenderer INSTANCE = new SodiumRenderer();

    public static final RenderMaterial STANDARD_MATERIAL = INSTANCE.materialFinder().find();

    static {
        INSTANCE.registerMaterial(RenderMaterial.STANDARD_ID, STANDARD_MATERIAL);
    }

    private final HashMap<ResourceLocation, RenderMaterial> materialMap = new HashMap<>();

    private SodiumRenderer() { }

    @Override
    public MutableMesh mutableMesh() {
        return new MutableMeshImpl();
    }

    @Override
    public MaterialFinder materialFinder() {
        return new MaterialFinderImpl();
    }

    @Override
    public RenderMaterial materialById(ResourceLocation id) {
        return materialMap.get(id);
    }

    @Override
    public boolean registerMaterial(ResourceLocation id, RenderMaterial material) {
        if (materialMap.containsKey(id)) return false;

        // cast to prevent acceptance of impostor implementations
        materialMap.put(id, (RenderMaterialImpl) material);
        return true;
    }

    @Override
    public void render(ModelBlockRenderer modelBlockRenderer, BlockAndTintGetter blockView, BlockStateModel model, BlockState state, BlockPos pos, PoseStack poseStack, MultiBufferSource multiBufferSource, boolean cull, long seed, int overlay) {
        if (!((FabricBlockStateModel) model).isVanillaAdapter()) {
            NonTerrainBlockRenderContext.POOL.get().renderModel(blockView, ((ModelBlockRendererAccessor) modelBlockRenderer).getBlockColors(), model, state, pos, poseStack, multiBufferSource, cull, seed, overlay);
        } else {
            VertexConsumer vertexConsumer = multiBufferSource.getBuffer(ItemBlockRenderTypes.getChunkRenderType(state));
            List<BlockModelPart> modelParts = model.collectParts(new SingleThreadedRandomSource(state.getSeed(pos)));
            modelBlockRenderer.tesselateBlock(blockView, modelParts, state, pos, poseStack, vertexConsumer, cull, overlay);
        }
    }

    @Override
    public void render(PoseStack.Pose entry, MultiBufferSource vertexConsumers, BlockStateModel model, float red, float green, float blue, int light, int overlay, BlockAndTintGetter blockView, BlockPos pos, BlockState state) {
        if (!((FabricBlockStateModel) model).isVanillaAdapter()) {
            SimpleBlockRenderContext.POOL.get().bufferModel(entry, vertexConsumers, model, red, green, blue, light, overlay, blockView, pos, state);
        } else {
            ModelBlockRenderer.renderModel(entry, vertexConsumers.getBuffer(ItemBlockRenderTypes.getChunkRenderType(state)), model, red, green, blue, light, overlay);
        }
    }

    @Override
    public void renderBlockAsEntity(BlockRenderDispatcher renderManager, BlockState state, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, BlockAndTintGetter blockView, BlockPos pos) {
        RenderShape renderShape = state.getRenderShape();

        if (renderShape != RenderShape.INVISIBLE) {
            BlockStateModel model = renderManager.getBlockModel(state);
            int tint = ((ModelBlockRendererAccessor) renderManager.getModelRenderer()).getBlockColors().getColor(state, null, null, 0);
            float red = (tint >> 16 & 255) / 255.0F;
            float green = (tint >> 8 & 255) / 255.0F;
            float blue = (tint & 255) / 255.0F;
            // TODO: seems wrong
            FabricBlockModelRenderer.render(poseStack.last(), layer -> multiBufferSource.getBuffer(RenderLayerHelper.getEntityBlockLayer(layer)), model, red, green, blue, light, overlay, blockView, pos, state);
            ((BlockRenderDispatcherAccessor) renderManager).getSpecialRenderers().get().renderByBlock(state.getBlock(), ItemDisplayContext.NONE, poseStack, multiBufferSource, light, overlay);
        }
    }
}
