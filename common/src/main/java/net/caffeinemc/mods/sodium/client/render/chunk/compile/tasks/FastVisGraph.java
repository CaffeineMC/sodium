package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import net.minecraft.Util;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.BitSet;

public class FastVisGraph {

    private int empty = 4096;

    private final BitSet bitSet = new BitSet(4096);

    private static final int DX = (int)Math.pow(16.0, 0.0);
    private static final int DZ = (int)Math.pow(16.0, 1.0);
    private static final int DY = (int)Math.pow(16.0, 2.0);

    private static final Direction[] VIS_DIRECTIONS = new Direction[] {
            Direction.WEST,
            Direction.EAST,
            Direction.DOWN,
            Direction.UP,
            Direction.NORTH,
            Direction.SOUTH,
    };
    private static final byte VIS_BIT_MINUS_X = 1 << 0;
    private static final byte VIS_BIT_PLUS_X = 1 << 1;
    private static final byte VIS_BIT_MINUS_Y = 1 << 2;
    private static final byte VIS_BIT_PLUS_Y = 1 << 3;
    private static final byte VIS_BIT_MINUS_Z = 1 << 4;
    private static final byte VIS_BIT_PLUS_Z = 1 << 5;
    private static final byte[] BASE_VISIBILITY = Util.make(new byte[4096], (visibility) -> {
        int index = 0;
        for(int y = 0; y < 16; y++) {
            for(int z = 0; z < 16; z++) {
                for(int x = 0; x < 16; x++) {
                    byte vis = 0;
                    if (x == 0) {
                        vis |= VIS_BIT_MINUS_X;
                    } else if (x == 15) {
                        vis |= VIS_BIT_PLUS_X;
                    } else if (y == 0) {
                        vis |= VIS_BIT_MINUS_Y;
                    } else if (y == 15) {
                        vis |= VIS_BIT_PLUS_Y;
                    } else if (z == 0) {
                        vis |= VIS_BIT_MINUS_Z;
                    } else if (z == 15) {
                        vis |= VIS_BIT_PLUS_Z;
                    }

                    visibility[index++] = vis;
                }
            }
        }
    });

    public void setOpaque(BlockPos blockPos) {
        this.bitSet.set(getIndex(blockPos), true);
        this.empty -= 1;
    }

    private static int getIndex(BlockPos blockPos) {
        return getIndex(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
    }

    private static int getIndex(int x, int y, int z) {
        return x | (z << 4) | (y << 8);
    }

    public VisibilitySet resolve() {
        VisibilitySet visibilitySet = new VisibilitySet();
        if (4096 - this.empty < 256) {
            visibilitySet.setAll(true);
        } else if (this.empty == 0) {
            visibilitySet.setAll(false);
        } else {
            byte[] visibility = BASE_VISIBILITY.clone();

            byte outputPlusX = 0;
            byte outputPlusY = 0;
            byte outputPlusZ = 0;
            byte outputMinusX = 0;
            byte outputMinusY = 0;
            byte outputMinusZ = 0;

            // Forwards pass
            int index = 0;
            for(int y = 0; y < 16; y++) {
                for(int z = 0; z < 16; z++) {
                    for(int x = 0; x < 16; x++) {
                        if (this.bitSet.get(index)) {
                            visibility[index] = 0;
                            index += 1;
                            continue;
                        }

                        byte vis = visibility[index];
                        if (vis == 0) {
                            index += 1;
                            continue;
                        }

                        if (x < 15) {
                            visibility[index + DX] |= (byte)(vis & ~VIS_BIT_PLUS_X);
                        } else {
                            outputPlusX |= (byte)(vis & ~VIS_BIT_PLUS_X);
                        }
                        if (z < 15) {
                            if (!this.bitSet.get(index + DZ)) {
                                visibility[index + DZ] |= (byte)(vis & ~VIS_BIT_PLUS_Z);
                                if (x > 0) {
                                    visibility[index + DZ - DX] |= (byte)(vis & ~VIS_BIT_PLUS_Z & ~VIS_BIT_MINUS_X);
                                } else {
                                    outputMinusX |= (byte)(vis & ~VIS_BIT_PLUS_Z & ~VIS_BIT_MINUS_X);
                                }
                            }
                        } else {
                            outputPlusZ |= (byte)(vis & ~VIS_BIT_PLUS_Z);
                        }
                        if (y < 15) {
                            if (!this.bitSet.get(index + DY)) {
                                visibility[index + DY] |= (byte)(vis & ~VIS_BIT_PLUS_Y);
                                if (z > 0) {
                                    if (!this.bitSet.get(index + DY - DZ)) {
                                        visibility[index + DY - DZ] |= (byte)(vis & ~VIS_BIT_PLUS_Y & ~VIS_BIT_MINUS_Z);
                                        if (x > 0) {
                                            visibility[index + DY - DZ - DX] |= (byte)(vis & ~VIS_BIT_PLUS_Y & ~VIS_BIT_MINUS_Z & ~VIS_BIT_MINUS_X);
                                        } else {
                                            outputMinusX |= (byte)(vis & ~VIS_BIT_PLUS_Y & ~VIS_BIT_MINUS_Z & ~VIS_BIT_MINUS_X);
                                        }
                                    }
                                } else {
                                    outputMinusZ |= vis;
                                }
                                if (x > 0) {
                                    visibility[index + DY - DX] |= (byte)(vis & ~VIS_BIT_PLUS_Y & ~VIS_BIT_MINUS_X);
                                } else {
                                    outputMinusX |= (byte)(vis & ~VIS_BIT_PLUS_Y & ~VIS_BIT_MINUS_X);
                                }
                            }
                        } else {
                            outputPlusY |= (byte)(vis & ~VIS_BIT_PLUS_Y);
                        }
                        index += 1;
                    }
                }
            }

            // Backwards pass
            index = 4095;
            for(int y = 15; y >= 0; y--) {
                for(int z = 15; z >= 0; z--) {
                    for(int x = 15; x >= 0; x--) {
                        if (this.bitSet.get(index)) {
                            visibility[index] = 0;
                            index -= 1;
                            continue;
                        }

                        byte vis = visibility[index];
                        if (vis == 0) {
                            index -= 1;
                            continue;
                        }

                        if (x > 0) {
                            visibility[index - DX] |= (byte)(vis & ~VIS_BIT_MINUS_X);
                        } else {
                            outputMinusX |= (byte)(vis & ~VIS_BIT_MINUS_X);
                        }
                        if (z > 0) {
                            if (!this.bitSet.get(index - DZ)) {
                                visibility[index - DZ] |= (byte)(vis & ~VIS_BIT_MINUS_Z);
                                if (x < 15) {
                                    visibility[index - DZ + DX] |= (byte)(vis & ~VIS_BIT_MINUS_Z & ~VIS_BIT_PLUS_X);
                                } else {
                                    outputPlusX |= (byte)(vis & ~VIS_BIT_MINUS_Z & ~VIS_BIT_PLUS_X);
                                }
                            }
                        } else {
                            outputMinusZ |= (byte)(vis & ~VIS_BIT_MINUS_Z);
                        }
                        if (y > 0) {
                            if (!this.bitSet.get(index - DY)) {
                                visibility[index - DY] |= (byte)(vis & ~VIS_BIT_MINUS_Y);
                                if (z < 15) {
                                    if (!this.bitSet.get(index - DY + DZ)) {
                                        visibility[index - DY + DZ] |= (byte)(vis & ~VIS_BIT_MINUS_Y & ~VIS_BIT_PLUS_Z);
                                        if (x < 15) {
                                            visibility[index - DY + DZ + DX] |= (byte)(vis & ~VIS_BIT_MINUS_Y & ~VIS_BIT_PLUS_Z & ~VIS_BIT_PLUS_X);
                                        } else {
                                            outputPlusX |= (byte)(vis & ~VIS_BIT_MINUS_Y & ~VIS_BIT_PLUS_Z & ~VIS_BIT_PLUS_X);
                                        }
                                    }
                                } else {
                                    outputPlusZ |= vis;
                                }
                                if (x < 15) {
                                    visibility[index - DY + DX] |= (byte)(vis & ~VIS_BIT_MINUS_Y & ~VIS_BIT_PLUS_X);
                                } else {
                                    outputPlusX |= (byte)(vis & ~VIS_BIT_MINUS_Y & ~VIS_BIT_PLUS_X);
                                }
                            }
                        } else {
                            outputMinusY |= (byte)(vis & ~VIS_BIT_MINUS_Y);
                        }
                        index -= 1;
                    }
                }
            }

            Direction plusX = Direction.EAST;
            Direction minusX = Direction.WEST;
            Direction plusY = Direction.UP;
            Direction minusY = Direction.DOWN;
            Direction plusZ = Direction.SOUTH;
            Direction minusZ = Direction.NORTH;

            for (int i = 0; i < VIS_DIRECTIONS.length; i++) {
                Direction visDirection = VIS_DIRECTIONS[i];
                int visIndex = 1 << i;

                if ((outputPlusX & visIndex) != 0) {
                    visibilitySet.set(plusX, visDirection, true);
                }
                if ((outputPlusY & visIndex) != 0) {
                    visibilitySet.set(plusY, visDirection, true);
                }
                if ((outputPlusZ & visIndex) != 0) {
                    visibilitySet.set(plusZ, visDirection, true);
                }
                if ((outputMinusX & visIndex) != 0) {
                    visibilitySet.set(minusX, visDirection, true);
                }
                if ((outputMinusY & visIndex) != 0) {
                    visibilitySet.set(minusY, visDirection, true);
                }
                if ((outputMinusZ & visIndex) != 0) {
                    visibilitySet.set(minusZ, visDirection, true);
                }
            }
        }

        return visibilitySet;
    }

}
