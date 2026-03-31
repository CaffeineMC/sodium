package net.caffeinemc.mods.sodium.client.render.immediate.model;

import com.mojang.math.Quadrant;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
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
import org.jspecify.annotations.NonNull;

import java.util.*;

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

            // Calculate the start coordinate and length of the side quad using the side face properties, as described
            // in the diagram in FaceStorage.
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
		var storage = new FaceStorage();

        // For each pixel in each frame, attempts to insert side faces of the pixel into the face storage.
        // All frames are included to avoid missing sides on animated textures with inconsistent shapes.
		sprite.getUniqueFrames().forEach(frame -> {
			for (var pixelY = 0; pixelY < height; pixelY ++) {
				for (var pixelX = 0; pixelX < width; pixelX ++) {
					storage.tryInsertPixel(
                            sprite,
                            frame,
                            pixelX,
                            pixelY,
                            width,
                            height
                    );
				}
			}
		});

        // Merge stored side faces.
		return storage.buildSideFaces();
	}

	private record ItemLayerKey(BakedQuad.MaterialInfo quadMaterial, ModelState modelState) implements ModelBaker.SharedOperationKey<@NotNull QuadCollection> {
        @Override
		public @NonNull QuadCollection compute(ModelBaker modelBakery) {
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

    /*Coordinates of the sprite:

    (0,0) ------ (width, 0)
      |
      |
      |
    (0, height)

    For SideDirection.UP/DOWN (plane horizontal)

       min(x) max(x)
         |      |
    +-----------+----+--
    |    |      |    ||\                                        ||
    |    |      |    || anchor(y)                               || Plane normal is vertical, parallel to the direction
    |    |      |    ||/                                        || vector of UP/DOWN.
    +----A------B----+-- <-- plane of anchor y                  \/
    +----------------+-- <-- plane of anchor y + v (v > 0)
    +----------------+
    So:
    The coordinate of the start point of the quad (A) is (min, anchor).
    The coordinate of the end point of the quad (B) is (max, anchor).
    Side quad AB is on the plane of anchor y.

    For SideDirection.LEFT/RIGHT (plane vertical):

    anchor(x)
    /     \
    |<--->|
    +-----+---+------+
    |     |   |      |
    |     A----------+-- min(y)   Plane normal is horizontal, parallel to the direction vector of LEFT/RIGHT.
    |     |   |      |               ----->
    |     B----------+-- max(y)
    |     |   |      |
    +-----+---+------+
          ^   ^
          |   plane of anchor x + v (v > 0)
    plane of anchor x
    So:
    The coordinate of the start point of the quad (A) is (anchor, min).
    The coordinate of the end point of the quad (B) is (anchor, max).
    Side quad AB is on the plane of anchor x.*/

    // Stores the side faces using BitSet maps. Each direction has its own map to avoid performance overhead of
    // enumerating all faces from different directions together.
    // Each map contains bit sets that represent "planes" of anchors in the same direction.
    // The bits in the bit set indicate which parts of the plane have per-pixel side quads.
    public record FaceStorage(
            Int2ObjectMap<BitSet> up,
            Int2ObjectMap<BitSet> down,
            Int2ObjectMap<BitSet> left,
            Int2ObjectMap<BitSet> right
    ) {
        public FaceStorage() {
            this(
                    new Int2ObjectOpenHashMap<>(),
                    new Int2ObjectOpenHashMap<>(),
                    new Int2ObjectOpenHashMap<>(),
                    new Int2ObjectOpenHashMap<>()
            );
        }

        public void tryInsertPixel(
                SpriteContents sprite,
                int frame,
                int pixelX,
                int pixelY,
                int width,
                int height
        ) {
            // If the pixel is transparent, any side quads would also be invisible.
            // Skip the transparent pixel to avoid generating redundant invisible quad faces.
            var opaque = !isTransparent(
                    sprite,
                    frame,
                    pixelX,
                    pixelY,
                    width,
                    height
            );

            if (opaque) {
                // Try insert per-pixel side quads for each side of the pixel.
                tryInsertFace(up, SideDirection.UP, sprite, frame, pixelX, pixelY, width, height);
                tryInsertFace(down, SideDirection.DOWN, sprite, frame, pixelX, pixelY, width, height);
                tryInsertFace(left, SideDirection.LEFT, sprite, frame, pixelX, pixelY, width, height);
                tryInsertFace(right, SideDirection.RIGHT, sprite, frame, pixelX, pixelY, width, height);
            }
        }

        public List<SideFace> buildSideFaces() {
            var output = new ReferenceArrayList<SideFace>();

            // Merges and collects all faces from different directions.
            buildMergedFaces(output, up, SideDirection.UP);
            buildMergedFaces(output, down, SideDirection.DOWN);
            buildMergedFaces(output, left, SideDirection.LEFT);
            buildMergedFaces(output, right, SideDirection.RIGHT);

            return output;
        }

        private static void tryInsertFace(
                Int2ObjectMap<BitSet> storage,
                SideDirection faceFacing,
                SpriteContents sprite,
                int frame,
                int pixelX,
                int pixelY,
                int width,
                int height
        ) {
            // Check if the neighbor pixel in the corresponding direction is transparent.
            var neighborTransparent = isTransparent(
                    sprite,
                    frame,
                    pixelX - faceFacing.getDirection().getStepX(),
                    pixelY - faceFacing.getDirection().getStepY(),
                    width,
                    height
            );

            // Only insert a per-pixel side quad if the side face is exposed (not blocked by opaque neighbors).
            if (neighborTransparent) {
                // Calculate the anchor and the pixel-level offset on the plane of the anchor,
                var anchor = faceFacing.isHorizontal() ? pixelY : pixelX;
                var offset = faceFacing.isHorizontal() ? pixelX : pixelY;

                // Mark the corresponding part of the plane of given anchor as occupied.
                storage.computeIfAbsent(anchor, _ -> new BitSet()).set(offset);
            }
        }

        /*
          <--merged-->    <-----merged----->
        001111111111110000111111111111111111
          ^           ^
          |           |
        min = ?     max = index - 1 = 14 - 1 = 13
        accum = 0   accum = 12
                    min = index - accum = 14 - 12 = 2
        SideFace(facing, anchor, min=2, max=13)
         */
        private static void buildMergedFaces(
                Collection<SideFace> faceOutput,
                Int2ObjectMap<BitSet> storage,
                SideDirection faceFacing
        ) {
            // Merge all planes (anchors) in the map.
            for (int anchor : storage.keySet()) {
                var faces = storage.get(anchor); // Get the plane bit set.
                var accum = 0; // Initialize the merge accumulation counter.

                // For all bits in the plane, scan the occupied bits (per-pixel side quads) and merge consecutive bits
                // into segments as SideFace.
                // The scan starts from the position (index) of the first per-pixel side quad (first set bit) to the
                // last per-pixel side quad (highest set bit).
                // The scan runs to faces.length() + 1 is to ensure that the final accumulated segment is also emitted,
                // since the bit immediately after the highest set bit is always clear.
                for (var index = faces.nextSetBit(0); index < faces.length() + 1; index ++) {
                    if (faces.get(index)) {
                        // The bit is set, accumulate the length of the segment.
                        accum ++;
                    } else {
                        // The bit is clear, meaning that the previous consecutive segment has ended, or a new segment
                        // has not started yet.
                        // If we do have a previously accumulated segment,
                        if (accum > 0) {
                            // Pop the segment out to the faceOutput as a merged SideFace.
                            faceOutput.add(new SideFace(
                                    faceFacing,
                                    index - accum,
                                    index - 1,
                                    anchor
                            ));
                        }

                        // Reset the accumulation counter for new segments.
                        accum = 0;
                    }
                }
            }
        }
    }

	public record SideFace(
			SideDirection facing,
			int min,
			int max,
			int anchor
	) {

	}
}
