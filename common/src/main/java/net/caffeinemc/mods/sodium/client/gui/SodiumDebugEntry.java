package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugColumn;
import net.minecraft.client.gui.components.debug.DebugGroup;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public class SodiumDebugEntry implements DebugScreenEntry {
    private static final DebugGroup DEBUG_GROUP = DebugGroup.Builder.titled("Sodium").withPreferredColumn(DebugColumn.Side.RIGHT).build();
    private final boolean verbose;

    public SodiumDebugEntry(boolean verbose) {
        this.verbose = verbose;
    }

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
        debugScreenDisplayer.addFactToGroup(DEBUG_GROUP, "Version", fact -> fact.formattedValue("%s%s", getVersionColor(), SodiumClientMod.getVersion()));

        var renderer = SodiumWorldRenderer.instanceNullable();

        if (renderer != null) {
            renderer.getDebugStrings(this.verbose, (x, i) -> debugScreenDisplayer.addFactToGroup(DEBUG_GROUP, x, i));
        }
    }
}
