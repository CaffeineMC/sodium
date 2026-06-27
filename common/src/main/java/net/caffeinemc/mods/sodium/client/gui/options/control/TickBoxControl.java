package net.caffeinemc.mods.sodium.client.gui.options.control;

import net.caffeinemc.mods.sodium.client.config.structure.BooleanOption;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.Screen;

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
        return Layout.TICKBOX_CONTROL_WIDTH;
    }

    @Override
    public StatefulOption<Boolean> getOption() {
        return this.option;
    }

    private static class TickBoxControlElement extends StatefulControlElement {
        private final BooleanOption option;

        public TickBoxControlElement(AbstractOptionList list, BooleanOption option, Dim2i dim, ColorTheme theme) {
            super(list, dim, theme);

            this.option = option;
        }

        @Override
        public BooleanOption getOption() {
            return this.option;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            if (!this.option.showControl() || this.isResetOverlayActive()) {
                return;
            }

            final int x = this.getLimitX() - Layout.OPTION_TEXT_SIDE_PADDING - Layout.CONTROL_ICON_SIZE;
            final int y = this.getCenterY() - Layout.CONTROL_ICON_SIZE / 2;
            final int xEnd = x + Layout.CONTROL_ICON_SIZE;
            final int yEnd = y + Layout.CONTROL_ICON_SIZE;

            final boolean enabled = this.option.isEnabled();
            final boolean ticked = this.option.getValidatedValue();

            final int color;

            if (enabled) {
                color = ticked ? this.theme.theme : Colors.FOREGROUND;
            } else {
                color = Colors.FOREGROUND_DISABLED;
            }

            if (ticked) {
                this.drawRect(graphics, x + 2, y + 2, xEnd - 2, yEnd - 2, color);
            }

            if (enabled) {
                this.drawBorder(graphics, x, y, xEnd, yEnd, color);
            } else {
                var size = 3;
                graphics.fill(x, y, x + size, y + 1, color);
                graphics.fill(x, y, x + 1, y + size, color);

                graphics.fill(xEnd - size, y, xEnd, y + 1, color);
                graphics.fill(xEnd - 1, y, xEnd, y + size, color);

                graphics.fill(x, yEnd - 1, x + size, yEnd, color);
                graphics.fill(x, yEnd - size, x + 1, yEnd, color);

                graphics.fill(xEnd - size, yEnd - 1, xEnd, yEnd, color);
                graphics.fill(xEnd - 1, yEnd - size, xEnd, yEnd, color);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) return true;
            if (this.isResetOverlayActive()) return false;

            if (this.option.isEnabled() && button == 0 && this.isMouseOver(mouseX, mouseY)) {
                this.toggleControl();
                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isFocused()) return false;

            if (CommonInputs.selected(keyCode)) {
                this.toggleControl();
                return true;
            }

            return false;
        }

        private void toggleControl() {
            this.playClickSound();

            this.option.modifyValue(!this.option.getValidatedValue());
        }
    }
}
