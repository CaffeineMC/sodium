package net.caffeinemc.mods.sodium.client.gui.options.control;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.StatefulOption;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.regex.Pattern;

public class IntegerTextBoxControl implements Control {
    private final IntegerOption option;

    public IntegerTextBoxControl(IntegerOption option) {
        this.option = option;
    }

    @Override
    public ControlElement createElement(Screen screen, AbstractOptionList list, Dim2i dim, ColorTheme theme) {
        return new IntegerTextBoxControlElement(list, this.option, dim, theme);
    }

    @Override
    public StatefulOption<Integer> getOption() {
        return this.option;
    }

    @Override
    public int getMaxWidth() {
        return Layout.SLIDER_WIDTH;
    }

    static class IntegerTextBoxControlElement extends StatefulControlElement {
        private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("[^0-9]");
        private static final int TEXT_BOX_WIDTH = Layout.SLIDER_WIDTH;
        private static final int TEXT_BOX_HEIGHT = Layout.BUTTON_SHORT - 6;

        private final IntegerOption option;
        private final EditBox textBox;

        private boolean updatingText;

        public IntegerTextBoxControlElement(AbstractOptionList list, IntegerOption option, Dim2i dim, ColorTheme theme) {
            super(list, dim, theme);

            this.option = option;

            this.textBox = new EditBox(
                    this.font,
                    0,
                    0,
                    TEXT_BOX_WIDTH - (Layout.INNER_MARGIN * 2),
                    TEXT_BOX_HEIGHT,
                    option.getName());
            this.textBox.setBordered(false);
            this.textBox.setMaxLength(String.valueOf(option.getSteppedValidator().max()).length());
            this.textBox.setResponder(this::setValueFromText);
            this.syncTextToOption();
        }

        @Override
        public IntegerOption getOption() {
            return this.option;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);

            if (!this.option.showControl() || this.isResetOverlayActive()) {
                return;
            }

            this.textBox.setEditable(this.option.isEnabled());
            this.textBox.setTextColor(this.option.isEnabled() ? Colors.FOREGROUND : Colors.FOREGROUND_DISABLED);
            this.textBox.setTextColorUneditable(Colors.FOREGROUND_DISABLED);

            if (!this.textBox.isFocused()) {
                this.syncTextToOption();
            }

            this.updateTextBoxPosition();

            int x = this.getTextBoxX();
            int y = this.getTextBoxY();
            int borderColor = (this.isFocused() || this.isMouseOverTextBox(mouseX, mouseY)) ? this.theme.themeLighter : Colors.BACKGROUND_LIGHT;

            this.drawRect(graphics, x, y, x + TEXT_BOX_WIDTH, y + TEXT_BOX_HEIGHT, Colors.BACKGROUND_MEDIUM);
            this.drawBorder(graphics, x, y, x + TEXT_BOX_WIDTH, y + TEXT_BOX_HEIGHT, borderColor);
            this.textBox.extractRenderState(graphics, mouseX, mouseY, delta);

            if (this.isMouseOverTextBox(mouseX, mouseY)) {
                graphics.requestCursor(CursorTypes.IBEAM);
            }
        }

        @Override
        public int getContentWidth() {
            return TEXT_BOX_WIDTH;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) {
                this.setFocused(false);
                this.syncTextToOption();
                return true;
            }

            if (this.isResetOverlayActive() || !this.option.isEnabled() || !this.option.showControl()) {
                return false;
            }

            this.updateTextBoxPosition();

            if (event.button() == 0 && this.isMouseOverTextBox(event.x(), event.y())) {
                this.setFocused(true);
                this.textBox.mouseClicked(event, doubleClick);
                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (!this.isFocused()) {
                return false;
            }

            if (event.isEscape() || event.isConfirmation()) {
                this.setFocused(false);
                return true;
            }

            return this.textBox.keyPressed(event);
        }

        @Override
        public boolean charTyped(CharacterEvent event) {
            if (!this.isFocused() || !isDigit(event.codepoint())) {
                return false;
            }

            return this.textBox.charTyped(event);
        }

        @Override
        public void setFocused(boolean focused) {
            if (focused) {
                this.focused = true;
                this.textBox.setFocused(true);
            } else {
                this.commitText();
                this.focused = false;
                this.textBox.setFocused(false);
            }
        }

        private int getTextBoxX() {
            return this.getLimitX() - TEXT_BOX_WIDTH - Layout.OPTION_TEXT_SIDE_PADDING;
        }

        private int getTextBoxY() {
            return this.getCenterY() - (TEXT_BOX_HEIGHT / 2);
        }

        private void updateTextBoxPosition() {
            this.textBox.setX(this.getTextBoxX() + Layout.INNER_MARGIN);
            this.textBox.setY(this.getTextBoxY() + ((TEXT_BOX_HEIGHT - this.font.lineHeight + 1) / 2));
        }

        private boolean isMouseOverTextBox(double mouseX, double mouseY) {
            int x = this.getTextBoxX();
            int y = this.getTextBoxY();
            return mouseX >= x && mouseX < x + TEXT_BOX_WIDTH && mouseY >= y && mouseY < y + TEXT_BOX_HEIGHT;
        }

        private void setValueFromText(String text) {
            if (this.updatingText) {
                return;
            }

            String sanitized = sanitize(text);
            if (!sanitized.equals(text)) {
                this.setText(sanitized);
                return;
            }

            if (sanitized.isEmpty()) {
                return;
            }

            this.option.modifyValue(this.getClampedValue(sanitized));
        }

        private void commitText() {
            String text = this.textBox.getValue();
            if (text.isEmpty()) {
                this.syncTextToOption();
                return;
            }

            int value = this.getClampedValue(text);
            this.option.modifyValue(value);
            this.setText(String.valueOf(value));
        }

        private int getClampedValue(String text) {
            var range = this.option.getSteppedValidator();
            int value;

            try {
                value = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                value = range.max();
            }

            return Mth.clamp(value, range.min(), range.max());
        }

        private void syncTextToOption() {
            this.setText(String.valueOf(this.option.getValidatedValue()));
        }

        private void setText(String text) {
            this.updatingText = true;
            this.textBox.setValue(text);
            this.updatingText = false;
        }

        private static String sanitize(String text) {
            return NON_DIGIT_PATTERN.matcher(text).replaceAll("");
        }

        private static boolean isDigit(int codepoint) {
            return codepoint >= '0' && codepoint <= '9';
        }
    }
}
