package net.caffeinemc.mods.sodium.client.gui.console;

import net.caffeinemc.mods.sodium.client.console.Console;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ConsoleHooks {
    public static void render(GuiGraphicsExtractor graphics, double currentTime) {
        ConsoleRenderer.INSTANCE.update(Console.INSTANCE, currentTime);
        ConsoleRenderer.INSTANCE.draw(graphics);
    }
}
