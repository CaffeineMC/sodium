package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Activates when its bound key is pressed with ALT held. While ALT is held, underlines the first occurrence of the shortcut key in the label; if the label doesn't contain the key (e.g. due to translation), appends the key in square brackets so the binding stays discoverable.
 */
public class KeyBoundButtonWidget extends FlatButtonWidget {
    private final int shortcutKey;
    private final Component underlinedLabel;

    public KeyBoundButtonWidget(Dim2i dim, Component label, Runnable action, boolean drawBackground, boolean leftAlign, int shortcutKey) {
        super(dim, label, action, drawBackground, leftAlign);
        this.shortcutKey = shortcutKey;
        this.underlinedLabel = buildUnderlinedLabel(label, shortcutKey);
    }

    private static Component buildUnderlinedLabel(Component label, int shortcutKey) {
        var text = label.getString();
        var keyChar = (char) shortcutKey; // GLFW letter key codes equal their uppercase ASCII char
        var index = indexOfIgnoreCase(text, keyChar);

        if (index >= 0) {
            return Component.empty()
                    .append(Component.literal(text.substring(0, index)))
                    .append(Component.literal(text.substring(index, index + 1)).withStyle(ChatFormatting.UNDERLINE))
                    .append(Component.literal(text.substring(index + 1)));
        }

        return Component.empty()
                .append(label)
                .append(Component.literal(" ["))
                .append(Component.literal(String.valueOf(keyChar)).withStyle(ChatFormatting.UNDERLINE))
                .append(Component.literal("]"));
    }

    private static int indexOfIgnoreCase(String text, char target) {
        var lowerTarget = Character.toLowerCase(target);
        for (int i = 0; i < text.length(); i++) {
            if (Character.toLowerCase(text.charAt(i)) == lowerTarget) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected Component getRenderedLabel() {
        return this.isEnabled() && Screen.hasAltDown() ? this.underlinedLabel : super.getRenderedLabel();
    }

    public boolean tryActivateShortcut(int keyCode, int modifiers) {
        if (this.isEnabled() && this.isVisible() && (modifiers & GLFW.GLFW_MOD_ALT) != 0 && keyCode == this.shortcutKey) {
            this.doAction();
            return true;
        }
        return false;
    }
}
