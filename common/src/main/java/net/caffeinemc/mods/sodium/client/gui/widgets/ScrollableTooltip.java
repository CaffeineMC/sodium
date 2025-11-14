package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

// TODO: is narration of the tooltip already handled by the screen or is there no narration at all?
public class ScrollableTooltip {
    private static final ResourceLocation ARROW_TEXTURE = ResourceLocation.fromNamespaceAndPath("sodium", "textures/gui/tooltip_arrows.png");
    private static final int ARROW_WIDTH = 5;
    private static final int SPRITE_WIDTH = 10;
    private static final int ARROW_HEIGHT = 9;

    private static final int MIN_TOOLTIP_WIDTH = 100;
    private static final int MAX_TOOLTIP_WIDTH = 200;
    private static final int TEXT_HORIZONTAL_PADDING = Layout.INNER_MARGIN - 1;
    private static final int TEXT_VERTICAL_PADDING = TEXT_HORIZONTAL_PADDING;
    private static final int LEFT_BOX_MARGIN = ARROW_WIDTH; // arrow includes one pixel of margin
    private static final int OUTER_BOX_MARGIN = 3;
    private static final int UPPER_BOX_MARGIN = Layout.BUTTON_SHORT + OUTER_BOX_MARGIN;

    private final Font font = Minecraft.getInstance().font;
    private ControlElement hoveredElement;
    private ScrollbarWidget scrollbar;
    private final Vector2i contentSize = new Vector2i();
    private Dim2i visibleDim;
    private boolean overlayMode;
    private final List<FormattedCharSequence> content = new ArrayList<>();
    private final VideoSettingsScreen parent;
    private final Vector2i reservedArea = new Vector2i(); // area reserved for action buttons

    public ScrollableTooltip(VideoSettingsScreen parent) {
        this.parent = parent;
    }

    public void onControlHover(ControlElement hovered, int mouseX, int mouseY) {
        if (hovered != null) {
            this.hoveredElement = hovered;

            if (this.scrollbar != null) {
                this.parent.removeWidget(this.scrollbar);
                this.scrollbar = null;
            }

            // re-flow the content if we need a scrollbar, which takes up some width
            if (this.positionTooltip(false)) {
                this.positionTooltip(true);

                this.scrollbar = this.parent.addRenderableWidget(new ScrollbarWidget(new Dim2i(
                        this.visibleDim.getLimitX() - Layout.SCROLLBAR_WIDTH,
                        this.visibleDim.y(),
                        Layout.SCROLLBAR_WIDTH,
                        this.visibleDim.height()
                ), false, true));
                this.scrollbar.setScrollbarContext(this.visibleDim.height(), this.contentSize.y());
            }
        } else if (this.hoveredElement != null) {
            this.positionTooltip(this.scrollbar != null);

            // handle the space between options and their tooltip
            if ((mouseX < this.hoveredElement.getLimitX() || mouseX >= this.visibleDim.x() ||
                    mouseY < this.hoveredElement.getY() || mouseY >= this.hoveredElement.getLimitY()) &&
                    !this.visibleDim.containsCursor(mouseX, mouseY)) {
                this.hoveredElement = null;

                if (this.scrollbar != null) {
                    this.parent.removeWidget(this.scrollbar);
                    this.scrollbar = null;
                }
            }
        }
    }

    private int getLineHeight() {
        return this.font.lineHeight + Layout.TEXT_LINE_SPACING;
    }

    private int generateTooltipContent(int boxWidth, boolean needsScrolling) {
        int textWidth = boxWidth - TEXT_HORIZONTAL_PADDING * 2;
        if (needsScrolling) {
            textWidth -= Layout.SCROLLBAR_WIDTH;
        }

        var option = this.hoveredElement.getOption();

        this.content.clear();
        this.content.addAll(this.font.split(option.getTooltip(), textWidth));

        OptionImpact impact = option.getImpact();
        if (impact != null) {
            var impactText = Component.translatable("sodium.options.performance_impact_string", impact.getName());
            this.content.addAll(this.font.split(impactText.withStyle(ChatFormatting.GRAY), textWidth));
        }

        return this.content.size() * this.getLineHeight() - Layout.TEXT_LINE_SPACING + TEXT_VERTICAL_PADDING * 2;
    }

    private boolean positionTooltip(boolean needsScrolling) {
        int elementLimitX = this.hoveredElement.getLimitX() + LEFT_BOX_MARGIN + Layout.SCROLLBAR_WIDTH;
        int defaultBoxWidth = Math.min(this.parent.width - elementLimitX - OUTER_BOX_MARGIN, MAX_TOOLTIP_WIDTH);
        int defaultBoxY = this.hoveredElement.getY();
        // noinspection UnnecessaryLocalVariable
        int defaultBoxX = elementLimitX;

        int boxWidth = 0, boxX = 0, boxY = 0;
        boolean fixedBoxY = false;
        int boxYCutoff = this.parent.height - OUTER_BOX_MARGIN;

        this.overlayMode = defaultBoxWidth < MIN_TOOLTIP_WIDTH;

        if (!this.overlayMode) {
            // Hovered element above the area so tooltip has full width, needs to be up high enough to not intersect with the area
            if (this.hoveredElement.getLimitY() < this.reservedArea.y) {
                boxWidth = defaultBoxWidth;
                boxX = defaultBoxX;
                boxY = defaultBoxY;

                // set the y cutoff to make sure the bottom of the tooltip is moved up above the reserved area
                boxYCutoff = this.reservedArea.y;
            }

            // Hovered element left of the area but enough space to show tooltip with reduced width
            else if (this.hoveredElement.getLimitX() < this.reservedArea.x) {
                int availableWidth = this.reservedArea.x - elementLimitX;

                if (availableWidth >= MIN_TOOLTIP_WIDTH) {
                    boxWidth = Math.min(availableWidth, MAX_TOOLTIP_WIDTH);
                    boxX = defaultBoxX;
                    boxY = defaultBoxY;
                } else {
                    this.overlayMode = true;
                }
            }

            // default non-overlay positioning
            else {
                boxWidth = defaultBoxWidth;
                boxX = defaultBoxX;
                boxY = defaultBoxY;
            }
        }

        if (this.overlayMode) {
            // in overlay mode the tooltip is shown on top of the options list, either above or below
            boxWidth = this.hoveredElement.getWidth() - 2 * OUTER_BOX_MARGIN;
            boxX = this.hoveredElement.getX() + OUTER_BOX_MARGIN;

            // place the content above or below the hovered element depending on which side has more space
            int spaceAbove = this.hoveredElement.getY() - UPPER_BOX_MARGIN;
            int spaceBelow = this.parent.height - this.hoveredElement.getLimitY() - OUTER_BOX_MARGIN;
            if (spaceBelow >= spaceAbove) {
                boxY = this.hoveredElement.getLimitY() + OUTER_BOX_MARGIN;
                boxYCutoff = this.parent.height - OUTER_BOX_MARGIN;
                
                // fix the box's upper y position since moving it up would cause it to overlap the hovered element
                fixedBoxY = true;
            } else {
                boxY = this.hoveredElement.getY() - OUTER_BOX_MARGIN;
                boxYCutoff = this.hoveredElement.getY() - OUTER_BOX_MARGIN;
                // it automatically gets moved up as far as necessary later
            }

        }

        int contentHeight = this.generateTooltipContent(boxWidth, needsScrolling);
        int boxYLimit = boxY + contentHeight;

        if (!fixedBoxY) {
            // If the box is going to be cut off on the Y-axis, move it back up the difference
            if (boxYLimit > boxYCutoff) {
                boxY -= boxYLimit - boxYCutoff;
            }

            // prevent it from moving up further than the tooltip safe area
            if (boxY < UPPER_BOX_MARGIN) {
                boxY = UPPER_BOX_MARGIN;
            }
        }

        this.contentSize.set(boxWidth, contentHeight);

        int maxVisibleHeight = boxYCutoff - boxY;
        int visibleHeight = Math.min(contentHeight, maxVisibleHeight);
        this.visibleDim = new Dim2i(boxX, boxY, boxWidth, visibleHeight);

        return contentHeight > maxVisibleHeight;
    }

    public void render(@NotNull GuiGraphics graphics) {
        if (this.hoveredElement == null) {
            return;
        }

        if (!this.overlayMode) {
            // draw small triangular arrow attached to the side of the tooltip box pointing at the hovered element, in the margin between the hovered element and the tooltip box
            int arrowX = this.visibleDim.x() - ARROW_WIDTH;
            int arrowY = this.hoveredElement.getCenterY() - (ARROW_HEIGHT / 2);

            graphics.nextStratum();

            // parameters are: render type, sprite, x, y, u offset, v offset, render width, render height, u size, v size, color
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, arrowX, arrowY, ARROW_WIDTH, 0, ARROW_WIDTH, ARROW_HEIGHT, SPRITE_WIDTH, ARROW_HEIGHT, Colors.BACKGROUND_LIGHT);
            graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, arrowX, arrowY, 0, 0, ARROW_WIDTH, ARROW_HEIGHT, SPRITE_WIDTH, ARROW_HEIGHT, Colors.BACKGROUND_DEFAULT);
        }

        int lineHeight = this.getLineHeight();

        int scrollAmount = 0;
        if (this.scrollbar != null) {
            scrollAmount = this.scrollbar.getScrollAmount();
        }

        var backgroundColor = this.overlayMode ? Colors.BACKGROUND_OVERLAY : Colors.BACKGROUND_LIGHT;

        graphics.enableScissor(this.visibleDim.x(), this.visibleDim.y(), this.visibleDim.getLimitX(), this.visibleDim.getLimitY());
        graphics.fill(this.visibleDim.x(), this.visibleDim.y(), this.visibleDim.getLimitX(), this.visibleDim.getLimitY(), backgroundColor);
        graphics.nextStratum();
        for (int i = 0; i < this.content.size(); i++) {
            graphics.drawString(this.font, this.content.get(i),
                    this.visibleDim.x() + TEXT_HORIZONTAL_PADDING, this.visibleDim.y() + TEXT_VERTICAL_PADDING + (i * lineHeight) - scrollAmount,
                    Colors.FOREGROUND);
        }
        graphics.disableScissor();
    }

    public boolean mouseScrolled(double d, double e, double amount) {
        if (this.visibleDim != null && this.visibleDim.containsCursor(d, e) && this.scrollbar != null) {
            this.scrollbar.scroll((int) (-amount * 10));
            return true;
        }
        return false;
    }

    /**
     * Sets the position of the top left corner of the reserved area that extends to the right and to the bottom to the screen edges.
     *
     * @param x The x coordinate of the top left corner of the reserved area
     * @param y The y coordinate of the top left corner of the reserved area
     */
    public void setReservedAreaTopLeftCorner(int x, int y) {
        this.reservedArea.set(x - OUTER_BOX_MARGIN, y - OUTER_BOX_MARGIN);
    }
}
