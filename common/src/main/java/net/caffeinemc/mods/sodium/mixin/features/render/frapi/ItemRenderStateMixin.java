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

import java.util.function.Consumer;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableMeshImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AccessLayerRenderState;
import net.caffeinemc.mods.sodium.client.render.frapi.render.QuadToPosPipe;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.class)
abstract class ItemRenderStateMixin {
    @Inject(method = "visitExtents(Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;setIdentity()V", shift = At.Shift.BEFORE))
    private void afterLayerLoad(Consumer<Vector3fc> posConsumer, CallbackInfo ci, @Local(ordinal = 0) Vector3f vec, @Local ItemStackRenderState.LayerRenderState layer, @Local Matrix4f matrix, @Share("pipe") LocalRef<QuadToPosPipe> pipeRef) {
        MutableMeshImpl mutableMesh = ((AccessLayerRenderState) layer).fabric_getMutableMesh();

        if (mutableMesh.size() > 0) {
            QuadToPosPipe pipe = pipeRef.get();

            if (pipe == null) {
                pipe = new QuadToPosPipe(posConsumer, vec);
                pipeRef.set(pipe);
            }
            pipe.matrix = matrix;
            // Use the mutable version here as it does not use a ThreadLocal or cursor stack
            mutableMesh.forEachMutable(pipe);
        }
    }
}