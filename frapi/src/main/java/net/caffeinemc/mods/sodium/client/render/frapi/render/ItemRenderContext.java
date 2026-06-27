package net.caffeinemc.mods.sodium.client.render.frapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.wrapper.MutableQuadViewWrapper;
import net.caffeinemc.mods.sodium.client.render.model.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.mixin.frapi.ItemFeatureRendererAccessor;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MeshView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricLayerRenderState;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricSubmitNodeCollection;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.LightCoordsUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Used during item buffering to support geometry added through {@link FabricLayerRenderState#emitter()}.
 */
public class ItemRenderContext {
	private final MutableQuadViewWrapper emitter;

    public ItemRenderContext() {
        this.emitter = new MutableQuadViewWrapper(null);

        var x = new MutableQuadViewImpl() {
            {
                this.data = new int[EncodingFormat.TOTAL_STRIDE];
                this.clear();
            }

            @Override
            public void emitDirectly() {
                ItemRenderContext.this.bufferQuad(ItemRenderContext.this.emitter);
            }
        };

        this.emitter.setDelegate(x);
    }

	private MultiBufferSource bufferSource;
	private OutlineBufferSource outlineBufferSource;
	private boolean translucent;

	private FabricSubmitNodeCollection.ExtendedItemSubmit submit;
	private PoseStack.@Nullable Pose foilDecalPose;

	public void prepare(MultiBufferSource.BufferSource bufferSource, OutlineBufferSource outlineBufferSource, boolean translucent) {
		this.bufferSource = bufferSource;
		this.outlineBufferSource = outlineBufferSource;
		this.translucent = translucent;
	}

	public void clear() {
        this.bufferSource = null;
        this.outlineBufferSource = null;
	}

	public void renderItem(FabricSubmitNodeCollection.ExtendedItemSubmit submit) {
		this.submit = submit;

		if (submit.outlineColor() != 0) {
            this.outlineBufferSource.setColor(submit.outlineColor());
		}

        this.bufferQuads(submit.quads(), submit.mesh());

        this.foilDecalPose = null;
	}

	private void bufferQuads(List<BakedQuad> vanillaQuads, MeshView mesh) {
		QuadEmitter emitter = this.emitter;
		emitter.clear();

		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < vanillaQuads.size(); i++) {
			final BakedQuad q = vanillaQuads.get(i);
			emitter.fromBakedQuad(q);
			emitter.emit();
		}

		mesh.outputTo(emitter);
	}

	private void bufferQuad(MutableQuadViewWrapper quad) {
		final RenderType renderType = quad.itemRenderType();


		if (renderType.hasBlending() != this.translucent) {
			return;
		}

        this.shadeQuad(quad, quad.emissive());
        this.tintQuad(quad);

		final FabricSubmitNodeCollection.ExtendedItemSubmit submit = this.submit;
		final ItemStackRenderState.FoilType foilType = quad.foilType() == null ? submit.foilType() : quad.foilType();

		if (foilType != ItemStackRenderState.FoilType.NONE) {
			final PoseStack.Pose foilDecalPose;

			if (foilType == ItemStackRenderState.FoilType.SPECIAL) {
				if (this.foilDecalPose == null) {
					this.foilDecalPose = ItemFeatureRendererAccessor.fabric_computeFoilDecalPose(submit.displayContext(), submit.pose());
				}

				foilDecalPose = this.foilDecalPose;
			} else {
				foilDecalPose = null;
			}

			final VertexConsumer foilBuffer = ItemFeatureRendererAccessor.fabric_getFoilBuffer(this.bufferSource, renderType, foilDecalPose);
			quad.buffer(submit.overlayCoords(), submit.pose(), foilBuffer);
		}

		if (submit.outlineColor() != 0) {
			quad.buffer(submit.overlayCoords(), submit.pose(), this.outlineBufferSource.getBuffer(renderType));
		}

		quad.buffer(submit.overlayCoords(), submit.pose(), this.bufferSource.getBuffer(renderType));
	}

	private void shadeQuad(MutableQuadViewWrapper quad, boolean emissive) {
		if (emissive) {
			quad.lightmap(LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT, LightCoordsUtil.FULL_BRIGHT);
		} else {
			quad.minLightmap(this.submit.lightCoords());
		}
	}

	private void tintQuad(MutableQuadViewWrapper quad) {
		final int tintIndex = quad.tintIndex();

		if (tintIndex >= 0 && tintIndex < this.submit.tintLayers().length) {
			quad.multiplyColor(this.submit.tintLayers()[tintIndex]);
		}
	}
}