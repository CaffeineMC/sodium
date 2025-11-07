
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

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableMeshImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AccessLayerRenderState;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ItemRenderContext;
import net.caffeinemc.mods.sodium.client.render.frapi.render.OrderedSubmitNodeCollectorExtension;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.renderer.v1.render.FabricLayerRenderState;

@Mixin(value = ItemStackRenderState.LayerRenderState.class)
public abstract class ItemLayerRenderStateMixin implements FabricLayerRenderState, AccessLayerRenderState {
    @Unique
    private final MutableMeshImpl mutableMesh = new MutableMeshImpl();

    @Inject(method = "clear()V", at = @At("RETURN"))
    private void onReturnClear(CallbackInfo ci) {
        mutableMesh.clear();
    }

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitItem(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;III[ILjava/util/List;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V"))
    private void submitItemProxy(SubmitNodeCollector instance, PoseStack poseStack, ItemDisplayContext displayContext, int light, int overlay, int outlineColor, int[] tints, List<BakedQuad> quads, RenderType layer, ItemStackRenderState.FoilType glint) {
        if (mutableMesh.size() > 0 && instance instanceof OrderedSubmitNodeCollectorExtension access) {
            // We don't have to copy the mesh here because vanilla doesn't copy the tint array or quad list either.
            access.fabric_submitItem(poseStack, displayContext, light, overlay, outlineColor, tints, quads, layer, glint, mutableMesh);
        } else {
            instance.submitItem(poseStack, displayContext, light, overlay, outlineColor, tints, quads, layer, glint);
        }
    }

    @Override
    public MutableMeshImpl fabric_getMutableMesh() {
        return mutableMesh;
    }
}