package net.caffeinemc.mods.sodium.client.world.biome;

import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.PalettedContainerROExtension;
import net.caffeinemc.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Arrays;

public class LevelBiomeSlice {
    private static final int SIZE = 3 * 4; // 3 chunks * 4 biomes per chunk

    // Arrays are in ZYX order
    @SuppressWarnings("unchecked")
    private final Holder<Biome>[][] biomes = new Holder[3 * 3 * 3 /* section count */][16 * 16 * 16 /* size */];

    private int blockX, blockY, blockZ;

    public void update(ClientLevel level, ChunkRenderContext context) {
        this.blockX = context.getOrigin().minBlockX() - 16;
        this.blockY = context.getOrigin().minBlockY() - 16;
        this.blockZ = context.getOrigin().minBlockZ() - 16;

        this.copyBiomeData(level, context);
    }

    private void copyBiomeData(Level level, ChunkRenderContext context) {
        var defaultValue = level.registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .getOrThrow(Biomes.PLAINS);

        for (int sectionX = 0; sectionX < 3; sectionX++) {
            for (int sectionY = 0; sectionY < 3; sectionY++) {
                for (int sectionZ = 0; sectionZ < 3; sectionZ++) {
                    this.copySectionBiomeData(context, sectionX, sectionY, sectionZ, defaultValue);
                }
            }
        }
    }

    private void copySectionBiomeData(ChunkRenderContext context, int sectionX, int sectionY, int sectionZ, Holder<Biome> defaultBiome) {
        var localIndex = LevelSlice.getLocalSectionIndex(sectionX, sectionY, sectionZ);
        var section  = context.getSections()[localIndex];
        var biomeData = section.getBiomeData();

        if (biomeData == null) {
            Arrays.fill(this.biomes[localIndex], defaultBiome);
        } else {
            PalettedContainerROExtension.of(biomeData).sodium$unpack(this.biomes[localIndex]);
        }
    }

    public Holder<Biome> getBiome(int blockX, int blockY, int blockZ) {
        int relBlockX = blockX - this.blockX;
        int relBlockY = blockY - this.blockY;
        int relBlockZ = blockZ - this.blockZ;

        return this.biomes[LevelSlice.getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)]
                [LevelSlice.getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15)];
    }
}
