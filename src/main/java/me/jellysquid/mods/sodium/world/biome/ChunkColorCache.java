package me.jellysquid.mods.sodium.world.biome;



import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.util.image.BoxBlur;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import java.util.Map;

public class ChunkColorCache {
    private static final int BORDER = 1;

    private final Biome[][] biomes;
    private final Map<ColorResolver, int[][]> colors;

    private final int sizeHorizontal;
    private final int sizeVertical;

    private final int blurHorizontal;

    private final int baseX, baseY, baseZ;

    private final SectionPos origin;
    private final BiomeManager biomeAccess;

    public ChunkColorCache(SectionPos origin, BiomeManager biomeAccess, int radius) {
        this.origin = origin;
        this.biomeAccess = biomeAccess;

        int borderXZ = radius + BORDER;
        int borderY = BORDER;

        this.sizeHorizontal = 16 + (borderXZ * 2);
        this.sizeVertical = 16 + (borderY * 2);

        this.blurHorizontal = radius;

        this.baseX = this.origin.minBlockX() - borderXZ;
        this.baseY = this.origin.minBlockY() - borderY;
        this.baseZ = this.origin.minBlockZ() - borderXZ;

        this.colors = new Reference2ReferenceOpenHashMap<>();
        this.biomes = new Biome[this.sizeVertical][];
    }

    public int getColor(ColorResolver resolver, int posX, int posY, int posZ) {
        var x = Mth.clamp(posX - this.baseX, 0, this.sizeHorizontal);
        var y = Mth.clamp(posY - this.baseY, 0, this.sizeVertical);
        var z = Mth.clamp(posZ - this.baseZ, 0, this.sizeHorizontal);

        int[][] colors = this.colors.get(resolver);

        if (colors == null) {
            this.colors.put(resolver, colors = new int[this.sizeVertical][]);
        }

        var layer = colors[y];

        if (layer == null) {
            colors[y] = (layer = this.gatherColorsXZ(resolver, y));
        }

        return layer[this.indexXZ(x, z)];
    }

    private Biome[] gatherBiomes(int level) {
        var biomeData = new Biome[this.sizeHorizontal * this.sizeHorizontal];

        var pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                biomeData[this.indexXZ(x, z)] = this.biomeAccess.getBiome(pos.set(x + this.baseX, level + this.baseY, z + this.baseZ));
            }
        }

        return biomeData;
    }

    private int[] gatherColorsXZ(ColorResolver resolver, int y) {
        var biomeData = this.getBiomeData(y);
        var colorData = new int[this.sizeHorizontal * this.sizeHorizontal];

        for (int x = 0; x < this.sizeHorizontal; x++) {
            for (int z = 0; z < this.sizeHorizontal; z++) {
                int index = this.indexXZ(x, z);
                colorData[index] = resolver.getColor(biomeData[index],
                        x + this.baseX, z + this.baseZ);
            }
        }

        BoxBlur.blur(colorData, this.sizeHorizontal, this.sizeHorizontal, this.blurHorizontal);

        return colorData;
    }

    private Biome[] getBiomeData(int y) {
        Biome[] biomes = this.biomes[y];

        if (biomes == null) {
            this.biomes[y] = (biomes = this.gatherBiomes(y));
        }

        return biomes;
    }

    private int indexXZ(int x, int z) {
        return (x * this.sizeHorizontal) + z;
    }
}
