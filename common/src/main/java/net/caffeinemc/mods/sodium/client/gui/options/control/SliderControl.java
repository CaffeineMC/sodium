package net.caffeinemc.mods.sodium.client.gui.options.control;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
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

    static class SliderControlElement extends ControlElement {
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
        public Option getOption() {
            return this.option;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            int sliderX = this.getSliderX();
            int sliderY = this.getSliderY();
            int sliderWidth = this.getSliderWidth();
            int sliderHeight = this.getSliderHeight();

            var value = this.option.getValidatedValue();
            var isEnabled = this.option.isEnabled();

            var label = this.option.formatValue(value);

            if (!isEnabled) {
                label = this.formatDisabledControlValue(label);
            }

            int labelWidth = this.font.width(label);

            boolean drawSlider = isEnabled && (this.hovered || this.isFocused());
            if (drawSlider) {
                this.contentWidth = sliderWidth + labelWidth;
            } else {
                this.contentWidth = labelWidth;
            }

            // render the label first and then the slider to prevent the highlight rect from darkening the slider
            super.extractRenderState(graphics, mouseX, mouseY, delta);

            if (!this.option.showControl()) {
                return;
            }

            if (drawSlider) {
                this.thumbPosition = this.getThumbPositionForValue(value);

                int thumbX = (int) (sliderX + this.thumbPosition * sliderWidth - THUMB_WIDTH);
                int trackY = (int) (sliderY + (sliderHeight / 2f) - ((double) TRACK_HEIGHT / 2));

                this.drawRect(graphics, sliderX, trackY, sliderX + sliderWidth, trackY + TRACK_HEIGHT, this.theme.themeLighter);
                this.drawRect(graphics, thumbX, sliderY, thumbX + (THUMB_WIDTH * 2), sliderY + sliderHeight, Colors.FOREGROUND);

                this.drawString(graphics, label, sliderX - labelWidth - 6, sliderY + (sliderHeight / 2) + Layout.REGULAR_TEXT_BASELINE_OFFSET, Colors.FOREGROUND);
            } else {
                this.drawString(graphics, label, sliderX + sliderWidth - labelWidth, sliderY + (sliderHeight / 2) + Layout.REGULAR_TEXT_BASELINE_OFFSET, Colors.FOREGROUND);
            }

            if (this.isMouseOverSlider(mouseX, mouseY)) {
                graphics.requestCursor(this.sliderHeld ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
            }
        }

        public int getSliderX() {
            return this.getLimitX() - 96;
        }

        public int getSliderY() {
            return this.getCenterY() - 5;
        }

        public int getSliderWidth() {
            return 90;
        }

        public int getSliderHeight() {
            return 10;
        }

        public boolean isMouseOverSlider(double mouseX, double mouseY) {
            return mouseX >= this.getSliderX() && mouseX < this.getSliderX() + this.getSliderWidth() && mouseY >= this.getSliderY() && mouseY < this.getSliderY() + this.getSliderHeight();
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
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            this.sliderHeld = false;

            if (this.option.isEnabled() && event.button() == 0 && this.isMouseOver(event.x(), event.y())) {
                if (this.isMouseOverSlider(event.x(), event.y())) {
                    this.setValueFromMouse(event.x());
                    this.sliderHeld = true;
                }

                return true;
            }

            return false;
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            if (this.option.isEnabled() && event.button() == 0 && this.sliderHeld) {
                this.sliderHeld = false;
                playClickSound();
                return true;
            }

            return false;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
            if (this.option.isEnabled() && event.button() == 0) {
                if (this.sliderHeld) {
                    this.setValueFromMouse(event.x());
                }

                return true;
            }

            return false;
        }

        private void setValueFromMouse(double d) {
            this.setValue(Mth.clamp((d - (double) this.getSliderX()) / (double) this.getSliderWidth(), 0.0D, 1.0D));
        }

        public void setValue(double newThumbPosition) {
            this.thumbPosition = newThumbPosition;

            this.option.modifyValue(this.getValueForThumbPosition());
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!isFocused()) return false;

            var range = this.option.getSteppedValidator();
            var isLeft = event.isLeft();
            var isRight = event.isRight();
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
