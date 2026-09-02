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
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.ExtendedMutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.MutableQuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.model.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.mixin.frapi.ItemFeatureRendererAccessor;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.submit.ExtendedItemSubmit;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class ExtendedItemFeatureRenderer extends RenderTypeFeatureRenderer<ExtendedItemSubmit> {
	private final MutableQuadViewImpl emitter = new MutableQuadViewImpl() {
		{
            this.data = new int[EncodingFormat.TOTAL_STRIDE];
            this.clear();
		}

		@Override
        public void emitDirectly() {
			switch (ExtendedItemFeatureRenderer.this.outputType) {
				case MAIN -> ExtendedItemFeatureRenderer.this.bufferMain(this);
				case OUTLINE -> ExtendedItemFeatureRenderer.this.bufferOutline(((ExtendedMutableQuadViewImpl) (Object) this).getWrapper());
			}
		}
	};

	private ExtendedItemSubmit submit;
	private PoseStack.@Nullable Pose foilDecalPose;
	private OutputType outputType;

	@Override
	protected void buildGroup(FeatureFrameContext context, List<ExtendedItemSubmit> submits) {
		for (ExtendedItemSubmit submit : submits) {
            this.prepareSubmit(submit);
		}

        this.submit = null;
        this.foilDecalPose = null;
	}

	private void prepareSubmit(ExtendedItemSubmit submit) {
		this.submit = submit;

		if (submit.outlineColor() != 0) {
            this.outputType = OutputType.OUTLINE;
		} else {
            foilDecalPose = null;
            this.outputType = OutputType.MAIN;
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
			quad.minLightmap(this.submit.lightCoords());
		}

		int tintIndex = quad.tintIndex();

		if (tintIndex >= 0 && tintIndex < this.submit.tintLayers().length) {
			quad.multiplyColor(this.submit.tintLayers()[tintIndex]);
		}

        ItemStackRenderState.FoilType quadFoilType = quad.foilType();
		ItemStackRenderState.FoilType foilType = quadFoilType == null ? this.submit.foilType() : quadFoilType;

        RenderType renderType = switch (foilType) {
            case NONE -> quad.itemRenderType();
            case STANDARD -> quad.itemGlintRenderType();
            case SPECIAL -> quad.itemGlintSpecialRenderType();
        };

        VertexConsumer vertexConsumer = getVertexBuilder(renderType);

		if (foilType == ItemStackRenderState.FoilType.SPECIAL) {
            if (foilDecalPose == null) {
                foilDecalPose = ItemFeatureRendererAccessor.fabric_computeFoilDecalPose(submit.displayContext(), submit.pose());
            }

            vertexConsumer = new SheetedDecalTextureGenerator(vertexConsumer, foilDecalPose, ItemFeatureRenderer.SPECIAL_FOIL_TEXTURE_SCALE);
		}

        quad.buffer(submit.overlayCoords(), submit.pose(), vertexConsumer);
	}


    private void bufferOutline(MutableQuadView quad) {
        RenderType renderType = quad.itemRenderType().outline().orElse(null);

        if (renderType != null) {
            int outlineColor = submit.outlineColor();
            quad.color(outlineColor, outlineColor, outlineColor, outlineColor);
            quad.buffer(submit.overlayCoords(), submit.pose(), getVertexBuilder(renderType));
        }


    }

    private enum OutputType {
        MAIN,
        OUTLINE

    }
}