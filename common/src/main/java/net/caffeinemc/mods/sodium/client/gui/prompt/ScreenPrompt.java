package net.caffeinemc.mods.sodium.client.gui.prompt;

import net.caffeinemc.mods.sodium.client.gui.ButtonTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget;
import net.caffeinemc.mods.sodium.client.gui.widgets.FlatButtonWidget;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class ScreenPrompt implements GuiEventListener, Renderable {
    private static final ButtonTheme PROMPT_THEME = new ButtonTheme(Colors.FOREGROUND, Colors.FOREGROUND, Colors.FOREGROUND, 0xff393939, 0xff2b2b2b, 0xff2b2b2b);

    private final ScreenPromptable parent;
    private final List<FormattedText> text;

    private final Action action;

    private FlatButtonWidget closeButton, actionButton;

    private final int width, height;

    public ScreenPrompt(ScreenPromptable parent, List<FormattedText> text, int width, int height, Action action) {
        this.parent = parent;
        this.text = text;

        this.width = width;
        this.height = height;

        this.action = action;
    }

    public void init() {
        var parentDimensions = this.parent.getDimensions();

        int boxX = (parentDimensions.width() / 2) - (this.width / 2);
        int boxY = (parentDimensions.height() / 2) - (this.height / 2);

        this.closeButton = new FlatButtonWidget(new Dim2i((boxX + this.width) - 84, (boxY + this.height) - 24, 80, Layout.BUTTON_SHORT), Component.literal("Close"), this::close, true, false, PROMPT_THEME);

        this.actionButton = new FlatButtonWidget(new Dim2i((boxX + this.width) - 198, (boxY + this.height) - 24, 110, Layout.BUTTON_SHORT), this.action.label, this::runAction, true, false, PROMPT_THEME);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        var parentDimensions = this.parent.getDimensions();

        graphics.fill(0, 0, parentDimensions.width(), parentDimensions.height(), 0x70090909);


        int boxX = (parentDimensions.width() / 2) - (this.width / 2);
        int boxY = (parentDimensions.height() / 2) - (this.height / 2);

        graphics.fill(boxX, boxY, boxX + this.width, boxY + this.height, 0xFF171717);
        graphics.renderOutline(boxX, boxY, this.width, this.height, 0xFF121212);


        int padding = 5;

        int textX = boxX + padding;
        int textY = boxY + padding;

        int textMaxWidth = this.width - (padding * 2);

        var font = Minecraft.getInstance().font;

        for (var paragraph : this.text) {
            var formatted = font.split(paragraph, textMaxWidth);

            for (var line : formatted) {
                graphics.drawString(font, line, textX, textY, Colors.FOREGROUND, true);
                textY += font.lineHeight + 2;
            }

            textY += Layout.TEXT_PARAGRAPH_SPACING;
        }

        for (var button : getWidgets()) {
            button.render(graphics, mouseX, mouseY, delta);
        }
    }

    @NonNull
    public List<AbstractWidget> getWidgets() {
        return List.of(this.actionButton, this.closeButton);
    }

    @Override
    public void setFocused(boolean focused) {
        if (focused) {
            this.parent.setPrompt(this);
        } else {
            this.parent.setPrompt(null);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (var widget : this.getWidgets()) {
            if (widget.mouseClicked(event, doubleClick)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.close();
            return true;
        }

        return GuiEventListener.super.keyPressed(event);
    }

    @Override
    public boolean isFocused() {
        return this.parent.getPrompt() == this;
    }

    private void close() {
        this.parent.setPrompt(null);
    }

    private void runAction() {
        this.action.runnable.run();
        this.close();
    }

    public record Action(Component label, Runnable runnable) {

    }
}
