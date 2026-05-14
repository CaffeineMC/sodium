package net.caffeinemc.mods.sodium.client.gui.screen;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

public class ConfigCorruptedScreen extends Screen {

    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 20;

    private static final int SCREEN_PADDING = 32;

    private final @Nullable Screen prevScreen;
    private final Function<Screen, Screen> nextScreen;

    public ConfigCorruptedScreen(@Nullable Screen prevScreen, @Nullable Function<Screen, Screen> nextScreen) {
        super(Component.translatable("sodium.console.corrupt_config.console.title"));

        this.prevScreen = prevScreen;
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = this.height - SCREEN_PADDING - BUTTON_HEIGHT;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.continue"), (btn) -> {
            Console.instance().logMessage(MessageLevel.INFO, "sodium.console.corrupt_config.console.config_file_was_reset", true, 3.0);

            SodiumClientMod.restoreDefaultOptions();
            Minecraft.getInstance().setScreen(this.nextScreen.apply(this.prevScreen));
        }).bounds(this.width - SCREEN_PADDING - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        this.addRenderableWidget(Button.builder(Component.literal("Go back"), (btn) -> {
            Minecraft.getInstance().setScreen(this.prevScreen);
        }).bounds(SCREEN_PADDING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawString(this.font, Component.literal("Sodium Renderer"), 32, 32, Colors.FOREGROUND);
        graphics.drawString(this.font, Component.translatable("sodium.console.corrupt_config.message.title"), 32, 48, 0xFFFF0000);

        var lines = Arrays.stream(Component.translatable("sodium.console.corrupt_config.message.body").getString().split("\n"))
                .map(Component::literal);

        var i = 0;
        for (Iterator<MutableComponent> it = lines.iterator(); it.hasNext(); ) {
            var line = it.next();
            i++;

            if (line.getString().isEmpty()) {
                continue;
            }

            graphics.drawString(this.font, line, 32, 68 + (i * 12), Colors.FOREGROUND);
        }
    }
}
