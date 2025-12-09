package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.ExternalButtonOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.function.Consumer;

public class ExternalButtonControl implements Control {
    public static final Component BASE_BUTTON_TEXT = Component.translatable("sodium.options.open_external_page_button");

    private final ExternalButtonOption option;
    private final Consumer<Screen> currentScreenConsumer;

    public ExternalButtonControl(ExternalButtonOption option, Consumer<Screen> currentScreenConsumer) {
        this.option = option;
        this.currentScreenConsumer = currentScreenConsumer;
    }

    @Override
    public Option getOption() {
        return this.option;
    }

    @Override
    public ControlElement createElement(Screen screen, AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        return new ExternalButtonControlElement(screen, list, dim, this.option, this.currentScreenConsumer, theme);
    }

    @Override
    public int getMaxWidth() {
        return Layout.BUTTON_LONG;
    }

    public static Component formatExternalButtonText(boolean enabled, ColorTheme theme) {
        if (enabled) {
            var enabledText = Component.empty();
            enabledText.append(BASE_BUTTON_TEXT.copy().withStyle(ChatFormatting.UNDERLINE));
            enabledText.append(Component.literal(" >").copy().withStyle(Style.EMPTY.withColor(theme.theme)));
            return enabledText;
        } else {
            return BASE_BUTTON_TEXT.copy().withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY);
        }
    }

    private static class ExternalButtonControlElement extends ControlElement {
        private final Screen screen;
        private final ExternalButtonOption option;
        private final Consumer<Screen> currentScreenConsumer;

        public ExternalButtonControlElement(Screen screen, AbstractOptionList list, Dim2i dim, ExternalButtonOption option, Consumer<Screen> currentScreenConsumer, ColorTheme theme) {
            super(list, dim, theme);

            this.screen = screen;
            this.option = option;
            this.currentScreenConsumer = currentScreenConsumer;
        }

        @Override
        public Option getOption() {
            return this.option;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            Component buttonText = formatExternalButtonText(this.option.isEnabled(), this.theme);

            this.drawString(graphics, buttonText,
                    this.getLimitX() - Layout.OPTION_TEXT_SIDE_PADDING - this.font.width(buttonText),
                    this.getCenterY() + Layout.REGULAR_TEXT_BASELINE_OFFSET,
                    Colors.FOREGROUND);
        }

        private void openScreen(Screen screen) {
            this.currentScreenConsumer.accept(screen);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.option.isEnabled() && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
                this.openScreen(this.screen);
                this.playClickSound();

                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!isFocused()) return false;

            if (event.isSelection()) {
                this.openScreen(this.screen);
                this.playClickSound();

                return true;
            }

            return false;
        }
    }
}
