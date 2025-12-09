package net.caffeinemc.mods.sodium.client.gui.widgets;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ExternalPage;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractScrollable;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class PageListWidget extends AbstractScrollable {
    private final VideoSettingsScreen parent;
    private EntryWidget selected;
    private final Reference2ReferenceMap<Page, PageEntryWidget> pageToWidget = new Reference2ReferenceOpenHashMap<>();

    public PageListWidget(Dim2i position, VideoSettingsScreen parent) {
        super(position);
        this.parent = parent;
        this.rebuild();
    }

    private void rebuild() {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();

        this.clearChildren();
        this.scrollbar = this.addRenderableChild(new ScrollbarWidget(new Dim2i(this.getLimitX() - Layout.SCROLLBAR_WIDTH, y, Layout.SCROLLBAR_WIDTH, height), false, false));

        int entryHeight = this.font.lineHeight * 2;
        var headerHeight = this.font.lineHeight * 3;
        int listHeight = 0;
        for (var modOptions : ConfigManager.CONFIG.getModOptions()) {
            if (modOptions.pages().isEmpty()) {
                continue;
            }

            var theme = modOptions.theme();

            // spacing above the mod title
            listHeight += Layout.TEXT_LINE_SPACING;
            var headerDim = new Dim2i(x, y + listHeight, width, headerHeight);
            var modHeaderStart = headerDim.y();
            CenteredFlatWidget header = new HeaderEntryWidget(headerDim, modOptions, theme);
            listHeight += headerHeight;

            this.addRenderableChild(header);

            for (Page page : modOptions.pages()) {
                CenteredFlatWidget button;
                Dim2i widgetDim = new Dim2i(x, y + listHeight, width, entryHeight);
                if (page instanceof OptionPage optionPage) {
                    var scrollTargetStart = widgetDim.y();
                    if (modHeaderStart != -1) {
                        scrollTargetStart = modHeaderStart; // scroll to the mod header if the page is the first in the mod
                        modHeaderStart = -1;
                    }
                    var pageWidget = new PageEntryWidget(widgetDim, optionPage, theme, scrollTargetStart);
                    button = pageWidget;
                    this.pageToWidget.put(optionPage, pageWidget);
                } else if (page instanceof ExternalPage externalPage) {
                    button = new ExternalPageEntryWidget(widgetDim, externalPage, theme);
                } else {
                    throw new IllegalStateException("Unknown page type: " + page.getClass());
                }

                listHeight += entryHeight;

                this.addRenderableChild(button);
            }
        }

        this.scrollbar.setScrollbarContext(listHeight + Layout.INNER_MARGIN);
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackgroundGradient(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        graphics.enableScissor(this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        super.render(graphics, mouseX, mouseY, delta);
        graphics.disableScissor();
    }

    public static void renderBackgroundGradient(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fillGradient(x1, y1, x2, y2, Colors.BACKGROUND_LIGHT, Colors.BACKGROUND_DEFAULT);
    }

    private void switchSelectedWidget(EntryWidget widget) {
        if (widget != this.selected) {
            if (this.selected != null) {
                this.selected.setSelected(false);
            }
            this.selected = widget;
            this.selected.setSelected(true);
        }

        // scroll into view if not currently visible in the page list
        int widgetTop = this.selected.getScrollTargetStart();
        int widgetBottom = widgetTop + this.selected.getHeight();
        int viewTop = this.getY() + this.scrollbar.getScrollAmount();
        int viewBottom = viewTop + this.getHeight();
        if (widgetTop < viewTop) {
            this.scrollbar.scrollTo(widgetTop - this.getY());
        } else if (widgetBottom > viewBottom) {
            this.scrollbar.scrollTo(widgetBottom - this.getY() - this.getHeight());
        }
    }

    public void switchSelected(Page page) {
        this.switchSelectedWidget(this.pageToWidget.get(page));
    }

    private class EntryWidget extends CenteredFlatWidget {
        EntryWidget(Dim2i dim, Component label, boolean isSelectable, ColorTheme theme) {
            super(dim, label, isSelectable, theme);
        }

        EntryWidget(Dim2i dim, Component label, Component subtitle, boolean isSelectable, ColorTheme theme) {
            super(dim, label, subtitle, isSelectable, theme);
        }

        @Override
        void onAction() {
        }

        public int getScrollTargetStart() {
            return super.getY();
        }

        @Override
        public int getY() {
            return super.getY() - PageListWidget.this.scrollbar.getScrollAmount();
        }
    }

    private class HeaderEntryWidget extends EntryWidget {
        private final Identifier icon;

        HeaderEntryWidget(Dim2i dim, ModOptions modOptions, ColorTheme theme) {
            super(dim, Component.literal(modOptions.name()), Component.literal(modOptions.version()), false, theme);
            this.icon = modOptions.icon();
        }

        @Override
        protected int renderIcon(GuiGraphics graphics, int textColor) {
            if (this.icon == null) {
                return super.renderIcon(graphics, textColor);
            }

            return VideoSettingsScreen.renderIconWithSpacing(graphics, this.icon, textColor,
                    this.getX(), this.getY(), this.getHeight(), Layout.ICON_MARGIN);
        }
    }

    private class PageEntryWidget extends EntryWidget {
        private final OptionPage page;
        private final int scrollTargetStart;

        PageEntryWidget(Dim2i dim, OptionPage page, ColorTheme theme, int scrollTargetStart) {
            super(dim, page.name(), true, theme);
            this.page = page;
            this.scrollTargetStart = scrollTargetStart;
        }

        @Override
        public int getScrollTargetStart() {
            return this.scrollTargetStart;
        }

        @Override
        void onAction() {
            PageListWidget.this.switchSelectedWidget(this);
            PageListWidget.this.parent.jumpToPage(this.page);
        }
    }

    private class ExternalPageEntryWidget extends EntryWidget {
        private final ExternalPage page;

        ExternalPageEntryWidget(Dim2i dim, ExternalPage page, ColorTheme theme) {
            super(dim, page.name(), true, theme);
            this.page = page;
        }

        @Override
        void onAction() {
            this.page.currentScreenConsumer().accept(PageListWidget.this.parent);
        }
    }
}
