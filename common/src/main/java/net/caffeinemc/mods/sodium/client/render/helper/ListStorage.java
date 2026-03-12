package net.caffeinemc.mods.sodium.client.render.helper;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;

import java.util.List;

public interface ListStorage {
    List<BlockStateModelPart> clearAndGet();
}
