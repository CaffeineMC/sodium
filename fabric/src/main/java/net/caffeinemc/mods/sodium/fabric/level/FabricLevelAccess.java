package net.caffeinemc.mods.sodium.fabric.level;

import net.caffeinemc.mods.sodium.client.services.PlatformLevelAccess;
import net.caffeinemc.mods.sodium.client.world.SodiumAuxiliaryLightManager;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

public class FabricLevelAccess implements PlatformLevelAccess {
    @Override
    public @Nullable Object getBlockEntityData(BlockEntity blockEntity) {
        return blockEntity.getRenderData();
    }

    @Override
    public @Nullable SodiumAuxiliaryLightManager getLightManager(LevelChunk chunk, SectionPos pos) {
        return null;
    }
}
