package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptionListWidget extends AbstractOptionList {
    private final OptionPage page;
    private final ColorTheme theme;

    public OptionListWidget(Screen screen, Dim2i dim, OptionPage page, ColorTheme theme) {
        super(dim);
        this.page = page;
        this.theme = theme;
        this.rebuild(screen);
    }

    private void rebuild(Screen screen) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth() - Layout.OPTION_LIST_SCROLLBAR_OFFSET - Layout.SCROLLBAR_WIDTH;
        int height = this.getHeight();

        int maxWidth = 0;

        this.clearChildren();
        this.scrollbar = this.addRenderableChild(new ScrollbarWidget(new Dim2i(x + width + Layout.OPTION_LIST_SCROLLBAR_OFFSET, y, Layout.SCROLLBAR_WIDTH, height)));

        int entryHeight = this.font.lineHeight * 2;
        int listHeight = 0;

        var header = new HeaderWidget(this, new Dim2i(x, y + listHeight, width, entryHeight), this.page.name().getString(), this.theme.themeLighter);
        this.addRenderableChild(header);

        maxWidth = Math.max(maxWidth, header.getContentWidth());
        listHeight += entryHeight + Layout.INNER_MARGIN;

        for (OptionGroup group : this.page.groups()) {
            // Add each option's control element
            for (Option option : group.options()) {
                var control = option.getControl();
                var element = control.createElement(screen,this, new Dim2i(x, y + listHeight, width, entryHeight), this.theme);

                this.addRenderableChild(element);
                this.controls.add(element);

                maxWidth = Math.max(maxWidth, element.getContentWidth());

                // Move down to the next option
                listHeight += entryHeight;
            }

            // Add padding beneath each option group
            listHeight += Layout.INNER_MARGIN;
        }

        this.scrollbar.setScrollbarContext(listHeight - Layout.INNER_MARGIN);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.enableScissor(this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        super.render(graphics, mouseX, mouseY, delta);
        graphics.disableScissor();
    }

    public static class HeaderWidget extends AbstractWidget {
        protected final AbstractOptionList list;
        private final String title;
        private final int themeColor;

        public HeaderWidget(AbstractOptionList list, Dim2i dim, String title, int themeColor) {
            super(dim);
            this.list = list;
            this.title = title;
            this.themeColor = themeColor;
        }

        public int getContentWidth() {
            return 70;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            this.hovered = this.isMouseOver(mouseX, mouseY);

            this.drawRect(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), Colors.BACKGROUND_DEFAULT);
            this.drawString(graphics, this.truncateLabelToFit(this.title), this.getX() + 6, this.getCenterY() - 4, this.themeColor);
        }

        private String truncateLabelToFit(String name) {
            return truncateTextToFit(name, this.getWidth() - 12);
        }

        @Override
        public int getY() {
            return super.getY() - this.list.getScrollAmount();
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
            return null;
        }
    }
}
