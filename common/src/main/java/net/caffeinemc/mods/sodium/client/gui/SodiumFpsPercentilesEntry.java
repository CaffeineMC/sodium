package net.caffeinemc.mods.sodium.client.gui;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

// This entry is added so that the F3+F6 menu can toggle SODIUM_FPS_PERCENTILES.
public class SodiumFpsPercentilesEntry implements DebugScreenEntry {
    @Override
    public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
    }

    @Override
    public boolean isAllowed(boolean reducedDebugInfo) {
        return true;
    }
}
