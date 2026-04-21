package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.ButtonTheme;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

// A button widget that updates its label dynamically when the ALT key is pressed.
// When active, the first character of the label is underlined to indicate a keybinding.
public class KeyBoundButtonWidget extends FlatButtonWidget {

    private boolean altPressed = false;
    private boolean labelNeedsRebuild = false;

    public KeyBoundButtonWidget(Dim2i dim, Component label, Runnable action, boolean drawBackground, boolean drawFrame, boolean leftAlign, ButtonTheme theme) {
        super(dim, label, action, drawBackground, drawFrame, leftAlign, theme);
    }

    public KeyBoundButtonWidget(Dim2i dim, Component label, Runnable action, boolean drawBackground, boolean leftAlign, ButtonTheme theme) {
        super(dim, label, action, drawBackground, leftAlign, theme);
    }

    public KeyBoundButtonWidget(Dim2i dim, Component label, Runnable action, boolean drawBackground, boolean leftAlign) {
        super(dim, label, action, drawBackground, leftAlign);
    }

    public KeyBoundButtonWidget(Dim2i dim, Component label, Runnable action, boolean drawBackground, boolean drawFrame, boolean leftAlign) {
        super(dim, label, action, drawBackground, drawFrame, leftAlign);
    }

    // Changes the first letter of a label to be underlined only whe ALT state changes.
    private Component buildLabel() {
        this.labelNeedsRebuild = false;

        String label = this.label.getString();

        if ((this.isAltDown() && this.isEnabled()) && !label.isEmpty()) {
            Component firstLetter = Component.literal(String.valueOf(label.charAt(0)))
                    .withStyle(style -> style.withUnderlined(true));

            Component restOfLabel = Component.literal(label.substring(1));

            return Component.literal("").append(firstLetter).append(restOfLabel);
        }

        return Component.literal(label);
    }

    private boolean isAltDown() {
        long windowHandle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS;
    }

    // Detects ALT press/release transitions to avoid unnecessary work every frame
    private void verifyAlt() {
        if (this.isAltDown() && !this.altPressed) {
            this.altPressed = true;
            this.labelNeedsRebuild = true;
        }

        if (!this.isAltDown() && this.altPressed) {
            this.altPressed = false;
            this.labelNeedsRebuild = true;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        this.verifyAlt();

        if (this.labelNeedsRebuild) {
            this.label = this.buildLabel();
        }
    }
}