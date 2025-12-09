package net.caffeinemc.mods.sodium.mixin.features.render.world.clouds;

import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

/**
 * This mixin slightly optimizes cloud rendering by using pointers directly instead of ByteBuffer API calls.
 */
@Mixin(CloudRenderer.class)
public abstract class CloudRendererMixin {
    @Shadow
    @Final
    private static int FLAG_INSIDE_FACE;
    @Shadow
    @Final
    private static int FLAG_USE_TOP_COLOR;
    @Shadow
    private CloudRenderer.@Nullable TextureData texture;

    @Shadow
    private static boolean isNorthEmpty(long l) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean isSouthEmpty(long l) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean isWestEmpty(long l) {
        throw new AssertionError();
    }

    @Shadow
    private static boolean isEastEmpty(long l) {
        throw new AssertionError();
    }

    /**
     * @author IMS
     * @reason Optimize cloud meshing
     */
    @Overwrite
    private void buildMesh(CloudRenderer.RelativeCameraPos relativeCameraPos, ByteBuffer byteBuffer, int cellX, int cellZ, boolean fancy, int radius) {
        if (this.texture != null) {
            long[] cells = this.texture.cells();
            int width = this.texture.width();
            int height = this.texture.height();

            long ptr = MemoryUtil.memAddress(byteBuffer);
            int cellIndex = byteBuffer.position() / 3;

            for (int ring = 0; ring <= 2 * radius; ++ring) {
                for (int dx = -ring; dx <= ring; ++dx) {
                    int dz = ring - Math.abs(dx);
                    if (dz >= 0 && dz <= radius && dx * dx + dz * dz <= radius * radius) {
                        if (dz != 0) {
                            cellIndex = sodium$addCellGeometryToBuffer(ptr, cellIndex, dx, -dz, relativeCameraPos, fancy, cellX, cellZ, cells, width, height);
                        }

                        cellIndex = sodium$addCellGeometryToBuffer(ptr, cellIndex, dx, dz, relativeCameraPos, fancy, cellX, cellZ, cells, width, height);
                    }
                }
            }

            byteBuffer.position(cellIndex * 3);
        }
    }

    private static int sodium$calculateTaxicabDistance(int x, int z) {
        return Math.abs(x) + Math.abs(z);
    }

    private static int sodium$addCellGeometryToBuffer(long ptr, int index,
                                                      int x,
                                                      int z,
                                                      CloudRenderer.@Nullable RelativeCameraPos orientation,
                                                      boolean fancy, int camX, int camZ, long[] cells, int texWidth, int texHeight) {
        int o = Math.floorMod(camX + x, texWidth);
        int p = Math.floorMod(camZ + z, texHeight);
        long faces = cells[o + p * texWidth];

        if (faces == 0) {
            return index;
        }

        int newIndex = index;

        if (fancy) {
            newIndex = sodium$emitCellGeometryExterior(ptr, newIndex, faces, orientation, x, z);

            if (sodium$calculateTaxicabDistance(x, z) <= 1) {
                newIndex = sodium$emitCellGeometryInterior(ptr, newIndex, x, z);
            }
        } else {
            sodium$encodeCellFace(ptr, newIndex, x, z, Direction.DOWN, FLAG_USE_TOP_COLOR);
            newIndex++;
        }

        return newIndex;
    }

    private static int sodium$emitCellGeometryInterior(long ptr, int index, int x, int z) {
        sodium$encodeCellFace(ptr, index, x, z, Direction.DOWN, FLAG_INSIDE_FACE);
        sodium$encodeCellFace(ptr, index + 1, x, z, Direction.UP, FLAG_INSIDE_FACE);
        sodium$encodeCellFace(ptr, index + 2, x, z, Direction.NORTH, FLAG_INSIDE_FACE);
        sodium$encodeCellFace(ptr, index + 3, x, z, Direction.SOUTH, FLAG_INSIDE_FACE);
        sodium$encodeCellFace(ptr, index + 4, x, z, Direction.WEST, FLAG_INSIDE_FACE);
        sodium$encodeCellFace(ptr, index + 5, x, z, Direction.EAST, FLAG_INSIDE_FACE);

        return index + 6;
    }

    private static void sodium$encodeCellFace(long ptr, long index, int x, int z, Direction direction, int extraData) {
        int flags = direction.get3DDataValue() | extraData;
        flags |= (x & 1) << 7;
        flags |= (z & 1) << 6;

        long ptrIndex = ptr + (index * 3);

        MemoryUtil.memPutByte(ptrIndex, (byte) (x >> 1));
        MemoryUtil.memPutByte(ptrIndex + 1, (byte) (z >> 1));
        MemoryUtil.memPutByte(ptrIndex + 2, (byte) flags);
    }

    private static int sodium$emitCellGeometryExterior(long ptr, int index, long faces, CloudRenderer.@Nullable RelativeCameraPos orientation, int x, int z) {
        int faceCount = index;

        if (orientation != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
            sodium$encodeCellFace(ptr, faceCount, x, z, Direction.UP, 0);
            faceCount += 1;
        }

        if (orientation != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
            sodium$encodeCellFace(ptr, faceCount, x, z, Direction.DOWN, 0);
            faceCount += 1;
        }

        if (isNorthEmpty(faces) && z > 0) {
            sodium$encodeCellFace(ptr, faceCount, x, z, Direction.NORTH, 0);
            faceCount += 1;
        }

        if (isSouthEmpty(faces) && z < 0) {
            sodium$encodeCellFace(ptr, faceCount, x, z, Direction.SOUTH, 0);
            faceCount += 1;
        }

        if (isWestEmpty(faces) && x > 0) {
            sodium$encodeCellFace(ptr, faceCount, x, z, Direction.WEST, 0);
            faceCount += 1;
        }

        if (isEastEmpty(faces) && x < 0) {
            sodium$encodeCellFace(ptr, faceCount, x, z, Direction.EAST, 0);
            faceCount += 1;
        }

        return faceCount;
    }
}
