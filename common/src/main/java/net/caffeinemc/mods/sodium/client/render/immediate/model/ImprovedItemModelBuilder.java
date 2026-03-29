package net.caffeinemc.mods.sodium.client.render.immediate.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.LAYERS;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.TEXTURE_SLOTS;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.SOUTH_FACE_UVS;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.NORTH_FACE_UVS;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.MIN_Z;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.MAX_Z;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.UV_SHRINK;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.SideDirection;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.isTransparent;

public class ImprovedItemModelBuilder implements UnbakedModel {
	@Override
	public TextureSlots.@NotNull Data textureSlots() {
		return TEXTURE_SLOTS;
	}

	@Override
	public UnbakedGeometry geometry() {
		return ImprovedItemModelBuilder::bake;
	}

	@Override
	public GuiLight guiLight() {
		return GuiLight.FRONT;
	}

	private static QuadCollection bake(
			TextureSlots textureSlots,
			ModelBaker modelBaker,
			ModelState modelState,
			ModelDebugName debugName
	) {
		var builder	= new QuadCollection.Builder();

		for (var index = 0; index < LAYERS.size(); index ++) {
			var material = textureSlots.getMaterial(LAYERS.get(index));

			if (material == null) {
				break;
			}

			var bakedMaterial = modelBaker.materials().get(material, debugName);
			var quadMaterial = BakedQuad.MaterialInfo.of(
					bakedMaterial,
					bakedMaterial.sprite().transparency(),
					index,
					true,
					0
			);

			builder.addAll(modelBaker.compute(new ItemLayerKey(quadMaterial, modelState)));
		}

		return builder.build();
	}

	private static void bakeItemQuads(
			QuadCollection.Builder builder,
			ModelBaker.Interner interner,
			BakedQuad.MaterialInfo materialInfo,
			ModelState modelState
	) {
		var material = interner.materialInfo(materialInfo);

		var from = new Vector3f(0.0F, 0.0F, MIN_Z);
		var to = new Vector3f(16.0F, 16.0F, MAX_Z);

		builder.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, SOUTH_FACE_UVS, Quadrant.R0, material, Direction.SOUTH, modelState, null));
		builder.addUnculledFace(FaceBakery.bakeQuad(interner, from, to, NORTH_FACE_UVS, Quadrant.R0, material, Direction.NORTH, modelState, null));

		bakeSideQuads(
				material,
				interner,
				builder,
				modelState
		);
	}

	private static void bakeSideQuads(
			BakedQuad.MaterialInfo materialInfo,
			ModelBaker.Interner interner,
			QuadCollection.Builder builder,
			ModelState modelState
	) {
		var sprite = materialInfo.sprite().contents();

		var xScale = 16.0F / sprite.width();
		var yScale = 16.0F / sprite.height();

		for (SideFace sideFace : buildSideFaces(sprite)) {
			var faceFacing = sideFace.facing();
			var faceAnchor = sideFace.anchor();
			var faceMin = sideFace.min();
			var faceMax = sideFace.max();

			float minX = faceFacing.isHorizontal() ? faceMin : faceAnchor;
			float minY = faceFacing.isHorizontal() ? faceAnchor : faceMin;
			float length = faceMax - faceMin + 1.0F;

			var u0 = 0.0F;
			var v0 = 0.0F;

			var u1 = 0.0F;
			var v1 = 0.0F;

			if (faceFacing.isHorizontal()) {
				u0 = minX + UV_SHRINK;
				v0 = minY + UV_SHRINK;
				u1 = minX + length - UV_SHRINK;
				v1 = minY + 1.0F - UV_SHRINK;
			} else {
				u0 = minX + UV_SHRINK;
				v0 = minY + length - UV_SHRINK;
				u1 = minX + 1.0F - UV_SHRINK;
				v1 = minY + UV_SHRINK;
			}

			var fromX = minX;
			var fromY = minY;
			var toX = minX;
			var toY = minY;

			switch (faceFacing) {
				case UP -> {
					toX = minX + length;
				}
				case LEFT -> {
					toY = minY + length;
				}
				case DOWN -> {
					fromY = minY + 1.0F;
					toY = minY + 1.0F;
					toX = minX + length;
				}
				case RIGHT -> {
					fromX = minX + 1.0F;
					toX = minX + 1.0F;
					toY = minY + length;
				}
			}

			fromX *= xScale;
			fromY *= yScale;
			toX *= xScale;
			toY *= yScale;

			fromY = 16.0F - fromY;
			toY = 16.0F - toY;

			switch (faceFacing) {
				case RIGHT -> fromX = toX;
				case DOWN -> fromY = toY;
				case LEFT -> toX = fromX;
				case UP -> toY = fromY;
				default -> throw new UnsupportedOperationException();
			}

			builder.addUnculledFace(
					FaceBakery.bakeQuad(
							interner,
							new Vector3f(fromX, fromY, MIN_Z),
							new Vector3f(toX, toY, MAX_Z),
							new CuboidFace.UVs(
									u0 * xScale,
									v0 * yScale,
									u1 * xScale,
									v1 * yScale
							),
							Quadrant.R0,
							materialInfo,
							faceFacing.getDirection(),
							modelState,
							null
					)
			);
		}
	}

	private static Collection<SideFace> buildSideFaces(SpriteContents sprite) {
		var width = sprite.width();
		var height = sprite.height();
		var sideFaces = new HashSet<SideFace>();

		sprite.getUniqueFrames().forEach(frame -> {
			for (var pixelY = 0; pixelY < height; pixelY ++) {
				for (var pixelX = 0; pixelX < width; pixelX ++) {
					var opaque = !isTransparent(
							sprite,
							frame,
							pixelX,
							pixelY,
							width,
							height
					);

					if (opaque) {
						tryInsertFace(SideDirection.UP, sideFaces, sprite, frame, pixelX, pixelY, width, height);
						tryInsertFace(SideDirection.DOWN, sideFaces, sprite, frame, pixelX, pixelY, width, height);
						tryInsertFace(SideDirection.LEFT, sideFaces, sprite, frame, pixelX, pixelY, width, height);
						tryInsertFace(SideDirection.RIGHT, sideFaces, sprite, frame, pixelX, pixelY, width, height);
					}
				}
			}
		});

		return sideFaces;
	}

	private static void tryInsertFace(
			SideDirection sideFacing,
			Set<SideFace> sideFaces,
			SpriteContents sprite,
			int frame,
			int pixelX,
			int pixelY,
			int width,
			int height
	) {
		var neighborTransparent = isTransparent(
				sprite,
				frame,
				pixelX - sideFacing.getDirection().getStepX(),
				pixelY - sideFacing.getDirection().getStepY(),
				width,
				height
		);

		if (neighborTransparent) {
			insertOrMergeFace(
					sideFaces,
					sideFacing,
					pixelX,
					pixelY
			);
		}
	}

	private static void insertOrMergeFace(
			Set<SideFace> sideFaces,
			SideDirection sideFacing,
			int pixelX,
			int pixelY
	) {
		var newFace = new SideFace(
				sideFacing,
				sideFacing.isHorizontal() ? pixelX : pixelY,
				sideFacing.isHorizontal() ? pixelY : pixelX
		);

		while (true) {
			var newAnchor = newFace.anchor();
			var newMin = newFace.min();
			var newMax = newFace.max();
			var merged = false;

			for (var oldFace : sideFaces) {
				var oldFacing = oldFace.facing();

                if (oldFacing != sideFacing) {
                    continue;
                }

				var oldAnchor = oldFace.anchor();

                if (newAnchor != oldAnchor) {
                    continue;
                }

				var oldMin = oldFace.min();
				var oldMax = oldFace.max();

				if (newMin == oldMax + 1) {
					merged = true;
					newFace = new SideFace(
							sideFacing,
							oldMin,
							newMax,
							newAnchor
					);
				}

				if (newMax == oldMin - 1) {
					merged = true;
					newFace = new SideFace(
							sideFacing,
							newMin,
							oldMax,
							newAnchor
					);
				}

				if (merged) {
					sideFaces.remove(oldFace);
					break;
				}
			}

			if (!merged) {
				sideFaces.add(newFace);
				break;
			}
		}
	}

	private record ItemLayerKey(BakedQuad.MaterialInfo quadMaterial, ModelState modelState) implements ModelBaker.SharedOperationKey<@NotNull QuadCollection> {
        @Override
		public QuadCollection compute(ModelBaker modelBakery) {
			var builder = new QuadCollection.Builder();

			bakeItemQuads(
					builder,
					modelBakery.interner(),
					this.quadMaterial,
					this.modelState
			);

			return builder.build();
		}
	}

	public record SideFace(
			SideDirection facing,
			int min,
			int max,
			int anchor
	) {
		public SideFace(
				SideDirection facing,
				int minMax,
				int anchor
		) {
			this(
					facing,
					minMax,
					minMax,
					anchor
			);
		}
	}
}
