package net.caffeinemc.mods.sodium.client.gui.prompt;

import net.caffeinemc.mods.sodium.client.gui.Dimensioned;
import org.jetbrains.annotations.Nullable;

public interface ScreenPromptable extends Dimensioned {
    void setPrompt(@Nullable ScreenPrompt prompt);

    @Nullable ScreenPrompt getPrompt();
}
