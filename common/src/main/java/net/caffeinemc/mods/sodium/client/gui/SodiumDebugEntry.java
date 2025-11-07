package net.caffeinemc.mods.sodium.client.gui;

import com.google.common.collect.Lists;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.util.MathUtil;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class SodiumDebugEntry implements DebugScreenEntry {
    private static final ResourceLocation DEBUG_GROUP = ResourceLocation.fromNamespaceAndPath("sodium", "debug_group");

    private static ChatFormatting getVersionColor() {
        String version = SodiumClientMod.getVersion();
        ChatFormatting color;

        if (version.contains("-local")) {
            color = ChatFormatting.RED;
        } else if (version.contains("-snapshot")) {
            color = ChatFormatting.LIGHT_PURPLE;
        } else {
            color = ChatFormatting.GREEN;
        }

        return color;
    }

    @Override
    public void display(DebugScreenDisplayer debugScreenDisplayer, @Nullable Level level, @Nullable LevelChunk levelChunk, @Nullable LevelChunk levelChunk2) {
        debugScreenDisplayer.addLine("%sSodium Renderer (%s)".formatted(getVersionColor(), SodiumClientMod.getVersion()));

        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer != null) {
            debugScreenDisplayer.addToGroup(DEBUG_GROUP, renderer.getDebugStrings());
        }
    }
}
