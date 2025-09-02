package net.caffeinemc.mods.sodium.client.gui.widgets;

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
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PageListWidget extends AbstractScrollable {
    private final VideoSettingsScreen parent;
    private CenteredFlatWidget selected;

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
        this.scrollbar = this.addRenderableChild(new ScrollbarWidget(new Dim2i(this.getLimitX() - Layout.SCROLLBAR_WIDTH, y, Layout.SCROLLBAR_WIDTH, height)));

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
            CenteredFlatWidget header = new HeaderEntryWidget(new Dim2i(x, y + listHeight, width, headerHeight), modOptions, theme);
            listHeight += headerHeight;

            this.addRenderableChild(header);

            for (Page page : modOptions.pages()) {
                CenteredFlatWidget button;
                if (page instanceof OptionPage optionPage) {
                    button = new PageEntryWidget(new Dim2i(x, y + listHeight, width, entryHeight), optionPage, theme);
                } else if (page instanceof ExternalPage externalPage) {
                    button = new ExternalPageEntryWidget(new Dim2i(x, y + listHeight, width, entryHeight), externalPage, theme);
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackgroundGradient(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        graphics.enableScissor(this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        super.render(graphics, mouseX, mouseY, delta);
        graphics.disableScissor();
    }

    public static void renderBackgroundGradient(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.fillGradient(x1, y1, x2, y2, Colors.BACKGROUND_LIGHT, Colors.BACKGROUND_DEFAULT);
    }

    private void switchSelected(CenteredFlatWidget widget) {
        if (this.selected != null) {
            this.selected.setSelected(false);
        }
        this.selected = widget;
        this.selected.setSelected(true);
    }

    public void switchSelected(Page page) {
        for (var child : this.children()) {
            if (child instanceof PageEntryWidget pageEntryWidget) {
                if (pageEntryWidget.page == page) {
                    this.switchSelected(pageEntryWidget);
                    return;
                }
            }
        }
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

        @Override
        public int getY() {
            return super.getY() - PageListWidget.this.scrollbar.getScrollAmount();
        }
    }

    private class HeaderEntryWidget extends EntryWidget {
        private final ResourceLocation icon;

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

        PageEntryWidget(Dim2i dim, OptionPage page, ColorTheme theme) {
            super(dim, page.name(), true, theme);
            this.page = page;
        }

        @Override
        void onAction() {
            PageListWidget.this.switchSelected(this);
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
