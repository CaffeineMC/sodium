package net.caffeinemc.mods.sodium.client.gui.options.control;

import com.mojang.blaze3d.platform.InputConstants;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

public class SliderControl implements Control {
    private final IntegerOption option;

    public SliderControl(IntegerOption option) {
        this.option = option;
    }

    @Override
    public ControlElement createElement(Screen screen, AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        return new SliderControlElement(list, this.option, dim, theme);
    }

    @Override
    public StatefulOption<Integer> getOption() {
        return this.option;
    }

    @Override
    public int getMaxWidth() {
        throw new UnsupportedOperationException("Not implemented");
    }

    static class SliderControlElement extends StatefulControlElement {
        private static final int THUMB_WIDTH = 2, TRACK_HEIGHT = 1;

        private final IntegerOption option;

        private double thumbPosition;
        private boolean sliderHeld;
        private int contentWidth;

        public SliderControlElement(AbstractOptionList list, IntegerOption option, Dim2i dim, ColorTheme theme) {
            super(list, dim, theme);

            this.option = option;

            this.thumbPosition = this.getThumbPositionForValue(option.getValidatedValue());
            this.sliderHeld = false;
        }

        @Override
        public IntegerOption getOption() {
            return this.option;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int sliderX = this.getSliderX();
            int sliderY = this.getSliderY();
            int sliderWidth = Layout.SLIDER_WIDTH;
            int sliderHeight = Layout.SLIDER_HEIGHT;

            var value = this.option.getValidatedValue();
            var isEnabled = this.option.isEnabled();

            var label = this.option.formatValue(value);

            if (!isEnabled) {
                label = this.formatDisabledControlValue(label);
            }

            int labelWidth = this.font.width(label);

            // render the label first and then the slider to prevent the highlight rect from darkening the slider
            super.render(graphics, mouseX, mouseY, delta);

            if (!this.option.showControl() || this.isResetOverlayActive()) {
                return;
            }

            boolean drawSlider = isEnabled && (this.hovered || this.isFocused());
            if (drawSlider) {
                this.contentWidth = sliderWidth + labelWidth;
            } else {
                this.contentWidth = labelWidth;
            }

            if (drawSlider) {
                this.thumbPosition = this.getThumbPositionForValue(value);

                int thumbX = (int) (sliderX + this.thumbPosition * sliderWidth - THUMB_WIDTH);
                int trackY = (int) (sliderY + (sliderHeight / 2f) - ((double) TRACK_HEIGHT / 2));

                this.drawRect(graphics, sliderX, trackY, sliderX + sliderWidth, trackY + TRACK_HEIGHT, this.theme.themeLighter);
                this.drawRect(graphics, thumbX, sliderY, thumbX + (THUMB_WIDTH * 2), sliderY + sliderHeight, Colors.FOREGROUND);

                this.drawString(graphics, label, sliderX - labelWidth - Layout.OPTION_TEXT_SIDE_PADDING, sliderY + (sliderHeight / 2) + Layout.REGULAR_TEXT_BASELINE_OFFSET, Colors.FOREGROUND);
            } else {
                this.drawString(graphics, label, sliderX + sliderWidth - labelWidth, sliderY + (sliderHeight / 2) + Layout.REGULAR_TEXT_BASELINE_OFFSET, Colors.FOREGROUND);
            }
        }

        public int getSliderX() {
            return this.getLimitX() - Layout.SLIDER_WIDTH - Layout.OPTION_TEXT_SIDE_PADDING;
        }

        public int getSliderY() {
            return this.getCenterY() - Layout.SLIDER_HEIGHT / 2;
        }

        public boolean isMouseOverSlider(double mouseX, double mouseY) {
            return mouseX >= this.getSliderX() && mouseX < this.getSliderX() + Layout.SLIDER_WIDTH && mouseY >= this.getSliderY() && mouseY < this.getSliderY() + Layout.SLIDER_HEIGHT;
        }

        @Override
        public int getContentWidth() {
            return this.contentWidth;
        }

        public double getThumbPositionForValue(int value) {
            var range = this.option.getSteppedValidator();
            int min = range.min();
            int max = range.max();
            return Mth.clamp((double) (value - min) / (max - min), 0.0d, 1.0d);
        }

        private int getValueForThumbPosition() {
            var range = this.option.getSteppedValidator();
            int step = range.step();
            int min = range.min();
            int max = range.max();
            return min + (step * (int) Math.round((this.thumbPosition * (max - min)) / step));
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.sliderHeld = false;

            if (super.mouseClicked(mouseX, mouseY, button)) return true;
            if (this.isResetOverlayActive()) return false;

            if (this.option.isEnabled() && button == 0 && this.isMouseOver(mouseX, mouseY)) {
                if (this.isMouseOverSlider((int) mouseX, (int) mouseY)) {
                    this.setValueFromMouse(mouseX);
                    this.sliderHeld = true;
                }

                return true;
            }

            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (this.option.isEnabled() && button == 0 && this.sliderHeld) {
                this.sliderHeld = false;
                this.playClickSound();
                return true;
            }

            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            if (this.option.isEnabled() && button == 0) {
                if (this.sliderHeld) {
                    this.setValueFromMouse(mouseX);
                }

                return true;
            }

            return false;
        }

        private void setValueFromMouse(double d) {
            this.setValue(Mth.clamp((d - (double) this.getSliderX()) / (double) Layout.SLIDER_WIDTH, 0.0D, 1.0D));
        }

        public void setValue(double newThumbPosition) {
            this.thumbPosition = newThumbPosition;

            this.option.modifyValue(this.getValueForThumbPosition());
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!this.isFocused()) return false;

            var range = this.option.getSteppedValidator();
            var isLeft = keyCode == InputConstants.KEY_LEFT;
            var isRight = keyCode == InputConstants.KEY_RIGHT;
            if (isLeft || isRight) {
                var validatedValue = this.option.getValidatedValue();
                var step = range.step();
                if (isLeft) {
                    validatedValue -= step;
                } else {
                    validatedValue += step;
                }
                this.option.modifyValue(validatedValue);
                this.option.getValidatedValue();
                return true;
            }

            return false;
        }
    }
}
