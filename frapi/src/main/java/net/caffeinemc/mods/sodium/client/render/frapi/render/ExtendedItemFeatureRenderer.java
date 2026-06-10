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

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.mixin.frapi.ItemFeatureRendererAccessor;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.submit.ExtendedItemSubmit;

public class ExtendedItemFeatureRenderer extends RenderTypeFeatureRenderer<ExtendedItemSubmit> {
	private final MutableQuadViewImpl emitter = new MutableQuadViewImpl() {
		{
			data = new int[EncodingFormat.TOTAL_STRIDE];
			clear();
		}

		@Override
        public void emitDirectly() {
			switch (outputType) {
				case MAIN -> bufferMain(this);
				case OUTLINE -> bufferOutline(this);
				case FOIL -> bufferFoil(this);
			}
		}
	};

	private ExtendedItemSubmit submit;
	private PoseStack.@Nullable Pose foilDecalPose;
	private OutputType outputType;

	@Override
	protected void buildGroup(FeatureFrameContext context, List<ExtendedItemSubmit> submits) {
		for (ExtendedItemSubmit submit : submits) {
			prepareSubmit(submit, false);
		}

		for (ExtendedItemSubmit submit : submits) {
			prepareSubmit(submit, true);
		}

		submit = null;
		foilDecalPose = null;
	}

	private void prepareSubmit(ExtendedItemSubmit submit, boolean foil) {
		this.submit = submit;

		if (foil) {
			foilDecalPose = null;
			outputType = OutputType.FOIL;
		} else if (submit.outlineColor() != 0) {
			outputType = OutputType.OUTLINE;
		} else {
			outputType = OutputType.MAIN;
		}

		QuadEmitter emitter = ((ExtendedMutableQuadViewImpl) this.emitter).getWrapper();
		emitter.clear();

		List<BakedQuad> vanillaQuads = submit.quads();

		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < vanillaQuads.size(); i++) {
			final BakedQuad q = vanillaQuads.get(i);
			emitter.fromBakedQuad(q);
			emitter.emit();
		}

		submit.mesh().outputTo(emitter);
	}

	private void bufferMain(MutableQuadViewImpl q) {
        var quad = ((ExtendedMutableQuadViewImpl) q).getWrapper();
		if (quad.emissive()) {
			quad.lightmap(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT);
		} else {
			quad.minLightmap(submit.lightCoords());
		}

		int tintIndex = quad.tintIndex();

		if (tintIndex >= 0 && tintIndex < submit.tintLayers().length) {
			quad.multiplyColor(submit.tintLayers()[tintIndex]);
		}

		quad.buffer(submit.overlayCoords(), submit.pose(), getVertexBuilder(quad.itemRenderType()));
	}

	private void bufferOutline(MutableQuadViewImpl q) {
        var quad = ((ExtendedMutableQuadViewImpl) q).getWrapper();

        RenderType renderType = quad.itemRenderType().outline().orElse(null);

		if (renderType != null) {
			int outlineColor = submit.outlineColor();
			quad.color(outlineColor, outlineColor, outlineColor, outlineColor);
			quad.buffer(submit.overlayCoords(), submit.pose(), getVertexBuilder(renderType));
		}
	}

	private void bufferFoil(MutableQuadViewImpl q) {
        var quad = ((ExtendedMutableQuadViewImpl) q).getWrapper();

        ItemStackRenderState.FoilType quadFoilType = quad.foilType();
		ItemStackRenderState.FoilType foilType = quadFoilType == null ? submit.foilType() : quadFoilType;

		if (foilType == ItemStackRenderState.FoilType.NONE) {
			return;
		}

		PoseStack.Pose foilDecalPose;

		if (foilType == ItemStackRenderState.FoilType.SPECIAL) {
			if (this.foilDecalPose == null) {
				this.foilDecalPose = ItemFeatureRendererAccessor.fabric_computeFoilDecalPose(submit.displayContext(), submit.pose());
			}

			foilDecalPose = this.foilDecalPose;
		} else {
			foilDecalPose = null;
		}

		VertexConsumer foilBuffer = getFoilBuffer(quad.itemRenderType(), foilDecalPose);
		quad.buffer(submit.overlayCoords(), submit.pose(), foilBuffer);
	}

	private VertexConsumer getFoilBuffer(RenderType renderType, PoseStack.@Nullable Pose foilDecalPose) {
		RenderType foilRenderType = ItemFeatureRendererAccessor.fabric_useTransparentGlint(renderType) ? RenderTypes.glintTranslucent() : RenderTypes.glint();
		VertexConsumer foilBuffer = getVertexBuilder(foilRenderType);

		if (foilDecalPose != null) {
			foilBuffer = new SheetedDecalTextureGenerator(foilBuffer, foilDecalPose, 0.0078125F);
		}

		return foilBuffer;
	}

	private enum OutputType {
		MAIN,
		OUTLINE,
		FOIL
	}
}