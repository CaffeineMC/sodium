package net.caffeinemc.mods.sodium.mixin.fabric;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.biome.LevelBiomeSlice;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelSlice.class)
public abstract class LevelSliceMixin implements FabricBlockView {
    @Shadow
    private BoundingBox volume;

    @Shadow
    private int originBlockX;

    @Shadow
    private int originBlockY;

    @Shadow
    private int originBlockZ;

    @Shadow
    @Final
    @Nullable
    private Int2ReferenceMap<Object>[] blockEntityRenderDataArrays;

    @Shadow
    public static int getLocalSectionIndex(int sectionX, int sectionY, int sectionZ) {
        throw new AssertionError();
    }

    @Shadow
    public static int getLocalBlockIndex(int blockX, int blockY, int blockZ) {
        throw new AssertionError();
    }

    @Shadow
    @Final
    private LevelBiomeSlice biomeSlice;

    @Override
    public @Nullable Object getBlockEntityRenderData(BlockPos pos) {
        if (!this.volume.isInside(pos.getX(), pos.getY(), pos.getZ())) {
            return null;
        }

        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        var blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4)];

        if (blockEntityRenderDataMap == null) {
            return null;
        }

        return blockEntityRenderDataMap.get(getLocalBlockIndex(relBlockX & 15, relBlockY & 15, relBlockZ & 15));
    }

    @Override
    public boolean hasBiomes() {
        return true;
    }

    @Override
    public Holder<Biome> getBiomeFabric(BlockPos pos) {
        return this.biomeSlice.getBiome(pos.getX(), pos.getY(), pos.getZ());
    }
}
