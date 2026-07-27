package net.caffeinemc.mods.sodium.mixin.platform.neoforge;


import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * This is a self-mixin to implement Forge interfaces into LevelSlice.
 */
@Mixin(LevelSlice.class)
public abstract class LevelSliceMixin implements BlockAndTintGetter {
    @Shadow
    @Final
    private SodiumAuxiliaryLightManager[] auxLightManager;

    @Shadow
    private int originBlockX, originBlockY, originBlockZ;

    @Shadow
    public static int getLocalSectionIndex(int sectionX, int sectionY, int sectionZ) {
        throw new IllegalStateException("Not shadowed!");
    }

    @Shadow
    public SodiumModelData getPlatformModelData(BlockPos pos) {
        throw new IllegalStateException("Not shadowed!");
    }

    @Override
    public @NonNull ModelData getModelData(@NonNull BlockPos pos) {
        SodiumModelData modelData = this.getPlatformModelData(pos);
        return modelData != null ? (ModelData) (Object) modelData : ModelData.EMPTY;
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        int relChunkX = pos.x() - (this.originBlockX >> 4);
        int relChunkZ = pos.z() - (this.originBlockZ >> 4);

        return (AuxiliaryLightManager) this.auxLightManager[getLocalSectionIndex(relChunkX, 0, relChunkZ)];
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(BlockPos pos) {
        int relBlockX = pos.getX() - this.originBlockX;
        int relBlockY = pos.getY() - this.originBlockY;
        int relBlockZ = pos.getZ() - this.originBlockZ;

        int localSectionIndex = getLocalSectionIndex(relBlockX >> 4, relBlockY >> 4, relBlockZ >> 4);
        return (AuxiliaryLightManager) this.auxLightManager[localSectionIndex];
    }
}
