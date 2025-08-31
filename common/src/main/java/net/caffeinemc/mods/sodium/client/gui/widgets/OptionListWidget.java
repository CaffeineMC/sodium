package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OptionListWidget extends AbstractOptionList {
    private final boolean showAllPages;
    private List<Option.OptionNameSource> filteredOptions = null;
    private final Map<String, SectionInfo> sectionInfoMap = new HashMap<>();
    private Consumer<SectionInfo> onSectionFocused;
    private SectionInfo lastFocusedSection;

    public record SectionInfo(ModOptions modOptions, OptionPage page, int startY, int endY) {
    }

    // Constructor for showing all pages
    public OptionListWidget(Screen screen, Dim2i dim) {
        super(dim);
        this.showAllPages = true;
        this.rebuild(screen);
    }

    public void setOnSectionFocused(Consumer<SectionInfo> onSectionFocused) {
        this.onSectionFocused = onSectionFocused;
    }

    public void setFilteredOptions(List<Option.OptionNameSource> filteredOptions) {
        this.filteredOptions = filteredOptions;
    }

    public void clearFilter() {
        this.filteredOptions = null;
    }

    public void rebuild(Screen screen) {
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth() - Layout.OPTION_LIST_SCROLLBAR_OFFSET - Layout.SCROLLBAR_WIDTH;
        int height = this.getHeight();

        this.clearChildren();
        this.controls.clear();
        this.sectionInfoMap.clear();
        this.scrollbar = this.addRenderableChild(new ScrollbarWidget(new Dim2i(x + width + Layout.OPTION_LIST_SCROLLBAR_OFFSET, y, Layout.SCROLLBAR_WIDTH, height)));

        int entryHeight = this.font.lineHeight * 2;
        int listHeight = 0;

        if (this.filteredOptions != null) {
            listHeight = this.renderFilteredOptions(screen, x, y, width, entryHeight, listHeight);
        } else {
            listHeight = this.renderAllPages(screen, x, y, width, entryHeight, listHeight);
        }

        this.scrollbar.setScrollbarContext(listHeight - Layout.INNER_MARGIN);
    }

    private int renderFilteredOptions(Screen screen, int x, int y, int width, int entryHeight, int startHeight) {
        int listHeight = startHeight;

        Option.OptionNameSource lastSource = null;
        for (var source : this.filteredOptions) {
            var option = source.getOption();
            var control = option.getControl();
            var modOptions = source.getModOptions();
            var page = source.getPage();
            var theme = modOptions.theme();

            // Add mod/page headers if necessary
            if (lastSource == null || lastSource.getModOptions() != modOptions) {
                var modHeader = new ModHeaderWidget(this, new Dim2i(x, y + listHeight, width, entryHeight), modOptions.name(), theme);
                this.addRenderableChild(modHeader);
                listHeight += entryHeight + Layout.INNER_MARGIN;
            }

            if (lastSource == null || lastSource.getPage() != page) {
                var pageHeader = new PageHeaderWidget(this, new Dim2i(x, y + listHeight, width, entryHeight), page.name().getString(), theme);
                this.addRenderableChild(pageHeader);
                listHeight += entryHeight + Layout.INNER_MARGIN;
            }

            // Add the option control
            var element = control.createElement(screen, this, new Dim2i(x, y + listHeight, width, entryHeight), theme);
            this.addRenderableChild(element);
            this.controls.add(element);
            listHeight += entryHeight;

            // Add group spacing
            if (lastSource != null && lastSource.getOptionGroup() != source.getOptionGroup()) {
                listHeight += Layout.INNER_MARGIN;
            }

            lastSource = source;
        }

        return listHeight;
    }

    private int renderAllPages(Screen screen, int x, int y, int width, int entryHeight, int startHeight) {
        int listHeight = startHeight;

        for (var modOptions : ConfigManager.CONFIG.getModOptions()) {
            if (modOptions.pages().isEmpty()) {
                continue;
            }

            var theme = modOptions.theme();

            // Add mod header
            var modHeader = new ModHeaderWidget(this, new Dim2i(x, y + listHeight, width, entryHeight), modOptions.name(), theme);
            this.addRenderableChild(modHeader);
            listHeight += entryHeight + Layout.INNER_MARGIN;

            for (var page : modOptions.pages()) {
                if (!(page instanceof OptionPage optionPage)) {
                    continue; // Skip external pages
                }

                int pageStartY = listHeight;

                // Add page header
                var pageHeader = new PageHeaderWidget(this, new Dim2i(x, y + listHeight, width, entryHeight), optionPage.name().getString(), theme);
                this.addRenderableChild(pageHeader);
                listHeight += entryHeight + Layout.INNER_MARGIN;

                for (OptionGroup group : optionPage.groups()) {
                    // Add group header if it has a name
                    if (group.name() != null) {
                        var groupHeader = new GroupHeaderWidget(this, new Dim2i(x, y + listHeight, width, entryHeight), group.name().getString(), theme);
                        this.addRenderableChild(groupHeader);
                        listHeight += entryHeight;
                    }

                    // Add each option's control element
                    for (Option option : group.options()) {
                        var control = option.getControl();
                        var element = control.createElement(screen, this, new Dim2i(x, y + listHeight, width, entryHeight), theme);

                        this.addRenderableChild(element);
                        this.controls.add(element);
                        listHeight += entryHeight;
                    }

                    // Add padding beneath each option group
                    listHeight += Layout.INNER_MARGIN;
                }

                // Store section info for navigation using page as key
                var sectionKey = modOptions.name() + ":" + optionPage.name().getString() + ":";
                var sectionInfo = new SectionInfo(modOptions, optionPage, pageStartY, listHeight);
                this.sectionInfoMap.put(sectionKey, sectionInfo);
            }
        }

        return listHeight;
    }

    public void jumpToPage(ModOptions modOptions, OptionPage page) {
        var sectionKey = modOptions.name() + ":" + page.name().getString();
        var sectionInfo = this.sectionInfoMap.get(sectionKey);
        if (sectionInfo != null) {
            this.scrollbar.scrollTo(sectionInfo.startY);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.enableScissor(this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        super.render(graphics, mouseX, mouseY, delta);
        graphics.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.updateSectionFocus();

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void updateSectionFocus() {
        if (this.onSectionFocused == null || !this.showAllPages) {
            return;
        }

        int viewportTop = this.getScrollAmount() + this.getY();
        int viewportMiddle = viewportTop + this.getHeight() / 2;

        // Find which section is currently in the middle of the viewport
        SectionInfo currentSection = null;
        for (SectionInfo section : this.sectionInfoMap.values()) {
            if (viewportMiddle >= section.startY && viewportMiddle <= section.endY) {
                currentSection = section;
                break;
            }
        }

        // Only notify if the section has changed
        if (currentSection != null && currentSection != this.lastFocusedSection) {
            this.lastFocusedSection = currentSection;
            this.onSectionFocused.accept(currentSection);
        }
    }

    private abstract static class HeaderWidget extends AbstractWidget {
        protected final AbstractOptionList list;
        protected final String title;
        protected final int themeColor;

        public HeaderWidget(AbstractOptionList list, Dim2i dim, String title, int themeColor) {
            super(dim);
            this.list = list;
            this.title = title;
            this.themeColor = themeColor;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            this.hovered = this.isMouseOver(mouseX, mouseY);

            this.drawRect(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), Colors.BACKGROUND_LIGHT);
            this.drawString(graphics, this.truncateLabelToFit(this.title), this.getX() + 6, this.getCenterY() - 4, this.themeColor);
        }

        protected String truncateLabelToFit(String name) {
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

    private static class ModHeaderWidget extends HeaderWidget {
        public ModHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, title, theme.themeLighter);
        }
    }

    private static class PageHeaderWidget extends HeaderWidget {
        public PageHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, title, theme.theme);
        }
    }

    private static class GroupHeaderWidget extends HeaderWidget {
        public GroupHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, title, Colors.FOREGROUND);
        }
    }
}
