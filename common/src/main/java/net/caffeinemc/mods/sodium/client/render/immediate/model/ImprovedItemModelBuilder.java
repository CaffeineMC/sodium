package net.caffeinemc.mods.sodium.client.render.immediate.model;

import com.mojang.math.Quadrant;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.minecraft.client.renderer.block.model.ItemModelGenerator.LAYERS;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.TEXTURE_SLOTS;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.SOUTH_FACE_UVS;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.NORTH_FACE_UVS;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.MIN_Z;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.MAX_Z;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.UV_SHRINK;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.SideDirection;
import static net.minecraft.client.renderer.block.model.ItemModelGenerator.isTransparent;

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
        var blockElements = new ArrayList<BlockElement>();

		for (var index = 0; index < LAYERS.size(); index ++) {
            var layer = LAYERS.get(index);
			var material = textureSlots.getMaterial(layer);

			if (material == null) {
				break;
			}

            bakeItemQuads(
                    blockElements,
                    modelBaker.sprites().get(material, debugName).contents(),
                    layer,
                    index
            );
		}

		return SimpleUnbakedGeometry.bake(blockElements, textureSlots, modelBaker, modelState, debugName);
	}

	private static void bakeItemQuads(
            List<BlockElement> blockElements,
			SpriteContents sprite,
            String layer,
            int index
	) {
       blockElements.add(new BlockElement(
                new Vector3f(0.0F, 0.0F, 7.5F),
                new Vector3f(16.0F, 16.0F, 8.5F),
                Map.of(
                        Direction.SOUTH, new BlockElementFace(null, index, layer, SOUTH_FACE_UVS, Quadrant.R0),
                        Direction.NORTH, new BlockElementFace(null, index, layer, NORTH_FACE_UVS, Quadrant.R0)
                )
        ));

		bakeSideQuads(
				blockElements,
				sprite,
				layer,
				index
		);
	}

	private static void bakeSideQuads(
            List<BlockElement> blockElements,
            SpriteContents sprite,
            String layer,
            int index
	) {
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

            blockElements.add(new BlockElement(
                    new Vector3f(fromX, fromY, MIN_Z),
                    new Vector3f(toX, toY, MAX_Z),
                    Map.of(faceFacing.getDirection(), new BlockElementFace(
                            null,
                            index,
                            layer,
                            new BlockElementFace.UVs(
                                    u0 * xScale,
                                    v0 * yScale,
                                    u1 * xScale,
                                    v1 * yScale
                            ),
                            Quadrant.R0
                    ))
            ));
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
