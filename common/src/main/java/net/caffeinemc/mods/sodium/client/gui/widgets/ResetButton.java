package net.caffeinemc.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Right-aligned reset overlay shown when the parent row is hovered while SHIFT is held.
 * It derives is positioning from the parent so it tracks the parent's scroll position automatically.
 */
public class ResetButton extends AbstractWidget {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("sodium", "textures/gui/reset_button.png");
    private static final int ICON_SIZE = 10;
    private static final int COLOR = 0xFFFF8C30;

    private final AbstractWidget parent;
    private final Runnable action;

    public ResetButton(AbstractWidget parent, Runnable action) {
        super(new Dim2i(0, 0, Layout.BUTTON_SHORT, 0));
        this.parent = parent;
        this.action = action;
    }

    public static boolean isShiftHeld() {
        return Minecraft.getInstance().hasShiftDown();
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

        int x = this.getCenterX() - ICON_SIZE / 2;
        int y = this.getCenterY() - ICON_SIZE / 2;

        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE, COLOR);
        graphics.requestCursor(CursorTypes.POINTING_HAND);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isShiftHeld() || event.button() != 0) {
            return false;
        }

        if (!this.parent.isMouseOver(event.x(), event.y()) || !this.isMouseOver(event.x(), event.y())) {
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
