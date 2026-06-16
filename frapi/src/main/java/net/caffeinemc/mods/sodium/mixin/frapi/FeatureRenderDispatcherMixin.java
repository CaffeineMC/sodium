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

package net.caffeinemc.mods.sodium.mixin.frapi;

import net.caffeinemc.mods.sodium.client.render.frapi.render.ExtendedBlockModelFeatureRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.render.ExtendedItemFeatureRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;

import net.fabricmc.fabric.api.client.renderer.v1.render.submit.ExtendedBlockModelSubmit;
import net.fabricmc.fabric.api.client.renderer.v1.render.submit.ExtendedItemSubmit;

@Mixin(FeatureRenderDispatcher.class)
abstract class FeatureRenderDispatcherMixin {
	@Shadow
	@Final
	private FeatureRendererMap featureRenderers;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void registerExtendedFeatureRenderers(CallbackInfo ci) {
		featureRenderers.put(
				ExtendedBlockModelSubmit.TYPE,
				new ExtendedBlockModelFeatureRenderer()
		);
		featureRenderers.put(
				ExtendedItemSubmit.TYPE,
				new ExtendedItemFeatureRenderer()
		);
	}
}