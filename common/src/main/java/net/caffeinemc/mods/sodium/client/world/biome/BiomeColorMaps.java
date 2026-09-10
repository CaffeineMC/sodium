package net.caffeinemc.mods.sodium.client.world.biome;

import net.minecraft.world.level.DryFoliageColor;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;

public final class BiomeColorMaps {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 256;

    private static final int INVALID_INDEX = -1;

    public static int getGrassColor(int index) {
        if (index == INVALID_INDEX || index >= GrassColor.pixels.length) {
            return GrassColor.getDefaultColor();
        }

        return GrassColor.pixels[index];
    }

    public static int getFoliageColor(int index) {
        if (index == INVALID_INDEX || index >= FoliageColor.pixels.length) {
            return FoliageColor.FOLIAGE_DEFAULT;
        }

        return FoliageColor.pixels[index];
    }

    public static int getDryFoliageColor(int index) {
        if (index == INVALID_INDEX || index >= DryFoliageColor.pixels.length) {
            return DryFoliageColor.FOLIAGE_DRY_DEFAULT;
        }

        return DryFoliageColor.pixels[index];
    }

    public static int getIndex(double temperature, double downfall) {
        downfall *= temperature;

        int x = (int) ((1.0D - temperature) * 255.0D);
        int y = (int) ((1.0D - downfall) * 255.0D);

        if (x < 0 || x >= WIDTH) {
            return INVALID_INDEX;
        }

        if (y < 0 || y >= HEIGHT) {
            return INVALID_INDEX;
        }

        return (y << 8) | x;
    }

    private BiomeColorMaps() {}
}
