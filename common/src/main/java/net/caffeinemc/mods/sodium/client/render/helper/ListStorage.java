package net.caffeinemc.mods.sodium.client.render.helper;

import net.minecraft.client.renderer.block.model.BlockModelPart;

import java.util.List;

public interface ListStorage {
    List<BlockModelPart> clearAndGet();
}
