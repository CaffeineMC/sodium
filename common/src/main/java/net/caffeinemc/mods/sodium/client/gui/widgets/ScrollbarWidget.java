package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.IntConsumer;

public class ScrollbarWidget extends AbstractWidget {
    private static final int COLOR = ColorABGR.pack(50, 50, 50, 150);
    private static final int HIGHLIGHT_COLOR = ColorABGR.pack(100, 100, 100, 150);

    private final boolean horizontal;
    private final boolean alwaysShow;

    private int visible;
    private int total;

    private int scrollAmount;
    private long lastScrollTime;
    private boolean dragging;
    private final IntConsumer onScrollChange;

    public ScrollbarWidget(Dim2i dim2i, IntConsumer onScrollChange) {
        this(dim2i, false, false, onScrollChange);
    }

    public ScrollbarWidget(Dim2i dim2i, boolean horizontal, boolean alwaysShow) {
        this(dim2i, horizontal, alwaysShow, null);
    }

    public ScrollbarWidget(Dim2i dim2i, boolean horizontal, boolean alwaysShow, IntConsumer onScrollChange) {
        super(dim2i);
        this.horizontal = horizontal;
        this.alwaysShow = alwaysShow;
        this.onScrollChange = onScrollChange;
    }

    public void setScrollbarContext(int visible, int total) {
        this.visible = visible;
        this.total = total;
        this.setScrollAndNotify(Math.max(0, Math.min(total - visible, this.scrollAmount)));
    }

    public void setScrollbarContext(int total) {
        this.setScrollbarContext(this.horizontal ? this.getWidth() : this.getHeight(), total);
    }

    public boolean canScroll() {
        return this.total > this.visible;
    }

    public void scroll(int amount) {
        this.scrollTo(this.scrollAmount + amount);
    }

    public void scrollTo(int target) {
        if (this.setScrollAndNotify(Math.max(0, Math.min(this.total - this.visible, target)))) {
            this.lastScrollTime = System.currentTimeMillis();
        }
    }

    public int getScrollAmount() {
        return this.scrollAmount;
    }

    private boolean setScrollAndNotify(int newScrollAmount) {
        if (newScrollAmount != this.scrollAmount) {
            this.scrollAmount = newScrollAmount;
            if (this.onScrollChange != null) {
                this.onScrollChange.accept(this.scrollAmount);
            }
            return true;
        }
        return false;
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!this.canScroll()) {
            return;
        }
        boolean isMouseOver = this.isMouseOver(mouseX, mouseY);
        if (isMouseOver) {
            this.lastScrollTime = Math.max(this.lastScrollTime, System.currentTimeMillis() - 500);
        }
        long time = System.currentTimeMillis();
        long scrollTimeDiff = time - this.lastScrollTime;
        if (this.alwaysShow || isMouseOver || this.dragging || scrollTimeDiff < 1000) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), COLOR);
            int x1, y1, x2, y2;
            if (this.horizontal) {
                x1 = this.getX() + this.getHighlightStart(this.getWidth());
                y1 = this.getY();
                x2 = x1 + this.getHighlightLength(this.getWidth());
                y2 = y1 + this.getHeight();
            } else {
                x1 = this.getX();
                y1 = this.getY() + this.getHighlightStart(this.getHeight());
                x2 = x1 + this.getWidth();
                y2 = y1 + this.getHighlightLength(this.getHeight());
            }
            graphics.fill(x1, y1, x2, y2, HIGHLIGHT_COLOR);
        }
    }

    private boolean isMouseOverHighlight(double mouseX, double mouseY) {
        int x1, y1, x2, y2;
        if (this.horizontal) {
            x1 = this.getX() + this.getHighlightStart(this.getWidth());
            y1 = this.getY();
            x2 = x1 + this.getHighlightLength(this.getWidth());
            y2 = y1 + this.getHeight();
        } else {
            x1 = this.getX();
            y1 = this.getY() + this.getHighlightStart(this.getHeight());
            x2 = x1 + this.getWidth();
            y2 = y1 + this.getHighlightLength(this.getHeight());
        }
        return mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
    }

    private int getHighlightStart(int length) {
        return (int) Math.round(((double) this.scrollAmount / this.total) * length);
    }

    private int getHighlightLength(int length) {
        return (int) Math.round(((double) this.visible / this.total) * length);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.isMouseOver(event.x(), event.y()) || !this.canScroll()) {
            return false;
        }
        if (this.isMouseOverHighlight(event.x(), event.y())) {
            this.dragging = true;
        } else {
            if (this.horizontal) {
                this.scroll(event.x() > this.getHighlightStart(this.getWidth()) ? this.getWidth() : -this.getWidth());
            } else {
                this.scroll(event.y() > this.getHighlightStart(this.getHeight()) ? this.getHeight() : -this.getHeight());
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.dragging = false;
        this.lastScrollTime = Math.max(this.lastScrollTime, System.currentTimeMillis() - 500);
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.dragging) {
            this.scroll((int) Math.round(this.horizontal ? deltaX : deltaY * ((double) this.total / this.visible)));
            return true;
        }
        return false;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        // no narration
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return null;
    }
}
