package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.widgets.OptionListWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class TickBoxControl implements Control {
    private final BooleanOption option;

    public TickBoxControl(BooleanOption option) {
        this.option = option;
    }

    @Override
    public ControlElement createElement(Screen screen, AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        return new TickBoxControlElement(list, this.option, dim, theme);
    }

    @Override
    public int getMaxWidth() {
        return 30;
    }

    @Override
    public StatefulOption<Boolean> getOption() {
        return this.option;
    }

    private static class TickBoxControlElement extends ControlElement {
        private final BooleanOption option;

        public TickBoxControlElement(AbstractOptionList list, BooleanOption option, Dim2i dim, ColorTheme theme) {
            super(list, dim, theme);

            this.option = option;
        }

        @Override
        public Option getOption() {
            return this.option;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            final int x = this.getLimitX() - 16;
            final int y = this.getCenterY() - 5;
            final int xEnd = x + 10;
            final int yEnd = y + 10;

            final boolean enabled = this.option.isEnabled();
            final boolean ticked = enabled && this.option.getValidatedValue();

            final int color;

            if (enabled) {
                color = ticked ? this.theme.theme : Colors.FOREGROUND;
            } else {
                color = Colors.FOREGROUND_DISABLED;
            }

            if (ticked) {
                this.drawRect(graphics, x + 2, y + 2, xEnd - 2, yEnd - 2, color);
            }

            this.drawBorder(graphics, x, y, xEnd, yEnd, color);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (this.option.isEnabled() && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
                toggleControl();
                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!isFocused()) return false;

            if (event.isSelection()) {
                toggleControl();
                return true;
            }

            return false;
        }

        private void toggleControl() {
            this.playClickSound();
            this.playClickSound();

            this.option.modifyValue(!this.option.getValidatedValue());
        }
    }
}
