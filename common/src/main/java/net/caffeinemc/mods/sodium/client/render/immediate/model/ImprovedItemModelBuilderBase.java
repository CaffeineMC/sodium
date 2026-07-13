package net.caffeinemc.mods.sodium.client.render.immediate.model;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.client.renderer.texture.SpriteContents;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.SideDirection;
import static net.minecraft.client.resources.model.cuboid.ItemModelGenerator.isTransparent;

public class ImprovedItemModelBuilderBase {

    public static Collection<SideFace> buildSideFaces(SpriteContents sprite) {
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

    // Stores the side faces using maps split by direction. Each direction has its own map to avoid performance overhead
    // of enumerating all faces from different directions together. Each map contains planes of anchors in the same
    // direction, and each plane tracks which parts have per-pixel side quads.
    public record FaceStorage(
            Int2ObjectMap<FacePlane> up,
            Int2ObjectMap<FacePlane> down,
            Int2ObjectMap<FacePlane> left,
            Int2ObjectMap<FacePlane> right
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
                tryInsertFace(this.up, SideDirection.UP, sprite, frame, pixelX, pixelY, width, height);
                tryInsertFace(this.down, SideDirection.DOWN, sprite, frame, pixelX, pixelY, width, height);
                tryInsertFace(this.left, SideDirection.LEFT, sprite, frame, pixelX, pixelY, width, height);
                tryInsertFace(this.right, SideDirection.RIGHT, sprite, frame, pixelX, pixelY, width, height);
            }
        }

        public List<SideFace> buildSideFaces() {
            var output = new ReferenceArrayList<SideFace>();

            // Merges and collects all faces from different directions.
            buildMergedFaces(output, this.up, SideDirection.UP);
            buildMergedFaces(output, this.down, SideDirection.DOWN);
            buildMergedFaces(output, this.left, SideDirection.LEFT);
            buildMergedFaces(output, this.right, SideDirection.RIGHT);

            return output;
        }

        private static void tryInsertFace(
                Int2ObjectMap<FacePlane> storage,
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
                // Calculate the anchor and the pixel-level offset on the plane of the anchor.
                var anchor = faceFacing.isHorizontal() ? pixelY : pixelX;
                var offset = faceFacing.isHorizontal() ? pixelX : pixelY;

                // Mark the corresponding part of the plane of given anchor as occupied.
                storage.computeIfAbsent(anchor, _ -> new FacePlane()).set(
                        offset,
                        frame,
                        getPixelColor(sprite, frame, pixelX, pixelY)
                );
            }
        }

        private static int getPixelColor(SpriteContents sprite, int frame, int pixelX, int pixelY) {
            var animatedTexture = sprite.animatedTexture;

            if (animatedTexture == null) {
                return sprite.originalImage.getPixel(pixelX, pixelY);
            }

            return sprite.originalImage.getPixel(
                    pixelX + animatedTexture.getFrameX(frame) * sprite.width(),
                    pixelY + animatedTexture.getFrameY(frame) * sprite.height()
            );
        }

        private static void buildMergedFaces(
                Collection<SideFace> faceOutput,
                Int2ObjectMap<FacePlane> storage,
                SideDirection faceFacing
        ) {
            // Merge all planes (anchors) in the map.
            for (int anchor : storage.keySet()) {
                var plane = storage.get(anchor); // Get the plane.
                var faces = plane.faces();
                var min = -1;
                var previous = -1;

                for (var index = faces.nextSetBit(0); index >= 0; index = faces.nextSetBit(index + 1)) {
                    if (min < 0) {
                        min = index;
                    } else if (index != previous + 1 || !plane.hasSameColors(previous, index)) {
                        faceOutput.add(new SideFace(faceFacing, min, previous, anchor));
                        min = index;
                    }

                    previous = index;
                }

                if (min >= 0) {
                    faceOutput.add(new SideFace(faceFacing, min, previous, anchor));
                }
            }
        }
    }

    private static class FacePlane {
        private final BitSet faces = new BitSet();
        private final Int2ObjectMap<Int2IntMap> colorsByIndex = new Int2ObjectOpenHashMap<>();

        public BitSet faces() {
            return this.faces;
        }

        public void set(int index, int frame, int color) {
            this.faces.set(index);
            this.colorsByIndex
                    .computeIfAbsent(index, _ -> new Int2IntOpenHashMap())
                    .put(frame, color);
        }

        public boolean hasSameColors(int firstIndex, int secondIndex) {
            var firstColors = this.colorsByIndex.get(firstIndex);
            var secondColors = this.colorsByIndex.get(secondIndex);

            if (firstColors == null || secondColors == null || firstColors.size() != secondColors.size()) {
                return false;
            }

            for (Int2IntMap.Entry entry : firstColors.int2IntEntrySet()) {
                if (!secondColors.containsKey(entry.getIntKey())) {
                    return false;
                }

                if (secondColors.get(entry.getIntKey()) != entry.getIntValue()) {
                    return false;
                }
            }

            return true;
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
