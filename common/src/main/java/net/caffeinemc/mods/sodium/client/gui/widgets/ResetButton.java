package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.gui.GuiTint;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Right-aligned reset overlay shown when the parent row is hovered while SHIFT is held.
 * It derives is positioning from the parent so it tracks the parent's scroll position automatically.
 */
public class ResetButton extends AbstractWidget {
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("sodium", "textures/gui/reset_button.png");
    private static final int ICON_SIZE = Layout.CONTROL_ICON_SIZE;
    private static final int COLOR = 0xFFFF8C30;

    private final AbstractWidget parent;
    private final Runnable action;

    public ResetButton(AbstractWidget parent, Runnable action) {
        super(new Dim2i(0, 0, Layout.BUTTON_SHORT, 0));
        this.parent = parent;
        this.action = action;
    }

    public static boolean isShiftHeld() {
        return Screen.hasShiftDown();
    }

    public boolean isActive() {
        return this.parent.isHovered() && isShiftHeld();
    }

    @Override
    public int getX() {
        return this.parent.getLimitX() - this.getWidth();
    }

    @Override
    public int getY() {
        return this.parent.getY();
    }

    @Override
    public int getHeight() {
        return this.parent.getHeight();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!this.isActive()) {
            return;
        }

        final int x = this.getCenterX() - ICON_SIZE / 2;
        final int y = this.getCenterY() - ICON_SIZE / 2;

        GuiTint.withTint(COLOR, () ->
                graphics.blit(ICON, x, y, ICON_SIZE, ICON_SIZE, 0.0f, 0.0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isShiftHeld() || button != 0) {
            return false;
        }

        if (!this.parent.isMouseOver(mouseX, mouseY) || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        this.action.run();
        this.playClickSound();
        return true;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        // Only reachable via SHIFT-hover + click.
        return null;
    }
}
