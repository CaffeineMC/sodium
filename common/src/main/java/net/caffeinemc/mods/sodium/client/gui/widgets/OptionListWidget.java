package net.caffeinemc.mods.sodium.client.gui.widgets;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.ModOptions;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.gui.options.control.AbstractOptionList;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class OptionListWidget extends AbstractOptionList {
    private List<Option.OptionNameSource> filteredOptions = null;
    private final Reference2ReferenceMap<OptionPage, SectionInfo> optionToSectionInfo = new Reference2ReferenceOpenHashMap<>();
    private final Consumer<OptionPage> onPageFocused;
    private SectionInfo lastFocusedSection;
    private boolean ignoreNextScrollUpdate = false;
    private int entryHeight;

    private record SectionInfo(ModOptions modOptions, OptionPage page, int startY, int endY, int scrollJumpTarget) {
    }

    public OptionListWidget(Screen screen, Dim2i dim, Consumer<OptionPage> onPageFocused) {
        super(dim.insetLeft(Layout.OPTION_GROUP_MARGIN));
        this.onPageFocused = onPageFocused;
        this.rebuild(screen);
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
        this.optionToSectionInfo.clear();
        this.scrollbar = this.addRenderableChild(new ScrollbarWidget(new Dim2i(x + width + Layout.OPTION_LIST_SCROLLBAR_OFFSET, y, Layout.SCROLLBAR_WIDTH, height), this::updateSectionFocus));

        this.entryHeight = this.font.lineHeight * 2;
        int listHeight;

        if (this.filteredOptions != null) {
            listHeight = this.renderFilteredOptions(screen, x, y, width);
        } else {
            listHeight = this.renderAllPages(screen, x, y, width);
        }

        this.updateSectionFocus(this.scrollbar.getScrollAmount());
        this.scrollbar.setScrollbarContext(listHeight);
    }

    private int renderFilteredOptions(Screen screen, int x, int y, int width) {
        int listHeight = -Layout.OPTION_MOD_MARGIN;

        Option.OptionNameSource lastSource = null;
        for (var source : this.filteredOptions) {
            var option = source.getOption();
            var control = option.getControl();
            var modOptions = source.getModOptions();
            var page = source.getPage();
            var theme = modOptions.theme();

            // Add mod header if mod has changed
            if (lastSource == null || lastSource.getModOptions() != modOptions) {
                listHeight += Layout.OPTION_MOD_MARGIN;
                var modHeader = new ModHeaderWidget(this, new Dim2i(x, y + listHeight, width, this.entryHeight), modOptions.name(), theme, modOptions.icon());
                this.addRenderableChild(modHeader);
                listHeight += this.entryHeight;
            }

            // Add page header if page has changed
            if (lastSource == null || lastSource.getPage() != page) {
                listHeight += Layout.OPTION_PAGE_MARGIN;
                var pageHeader = new PageHeaderWidget(this, new Dim2i(x, y + listHeight, width, this.entryHeight), page.name().getString(), theme);
                this.addRenderableChild(pageHeader);
                listHeight += this.entryHeight;
            }

            // Add group spacing only if this isn't the first option after a page header
            if (lastSource == null || lastSource.getOptionGroup() != source.getOptionGroup()) { 
                listHeight += Layout.OPTION_GROUP_MARGIN;
            }

            // add the option control itself
            var element = control.createElement(screen, this, new Dim2i(x, y + listHeight, width, this.entryHeight).insetLeft(Layout.OPTION_LEFT_INSET), theme);
            this.addRenderableChild(element);
            this.controls.add(element);
            listHeight += this.entryHeight;

            lastSource = source;
        }

        return listHeight;
    }

    private int renderAllPages(Screen screen, int x, int y, int width) {
        int listHeight = -Layout.OPTION_MOD_MARGIN;

        for (var modOptions : ConfigManager.CONFIG.getModOptions()) {
            if (modOptions.pages().isEmpty()) {
                continue;
            }

            var theme = modOptions.theme();

            // Add mod header
            listHeight += Layout.OPTION_MOD_MARGIN;
            var modHeaderStart = listHeight;
            var modHeader = new ModHeaderWidget(this, new Dim2i(x, y + listHeight, width, this.entryHeight), modOptions.name(), theme, modOptions.icon());
            this.addRenderableChild(modHeader);
            listHeight += this.entryHeight;

            for (var page : modOptions.pages()) {
                if (!(page instanceof OptionPage optionPage)) {
                    continue; // there's nothing to render for non-option pages
                }

                int pageStartY = listHeight;

                // Add page header
                listHeight += Layout.OPTION_PAGE_MARGIN;
                var pageHeader = new PageHeaderWidget(this, new Dim2i(x, y + listHeight, width, this.entryHeight), optionPage.name().getString(), theme);
                this.addRenderableChild(pageHeader);
                listHeight += this.entryHeight;

                // removes the initial margin between the page header and the first group
                // listHeight -= Layout.OPTION_GROUP_MARGIN;
                // listHeight += Layout.OPTION_PAGE_MARGIN - Layout.OPTION_GROUP_MARGIN;

                for (OptionGroup group : optionPage.groups()) {
                    // Add padding beneath each option group
                    listHeight += Layout.OPTION_GROUP_MARGIN;

                    // Add group header if it has a name
                    if (group.name() != null) {
                        var groupHeader = new GroupHeaderWidget(this, new Dim2i(x, y + listHeight, width, this.entryHeight).insetLeft(Layout.OPTION_LEFT_INSET), group.name().getString(), theme);
                        this.addRenderableChild(groupHeader);
                        listHeight += this.entryHeight;
                    }

                    // Add each option's control element
                    for (Option option : group.options()) {
                        var control = option.getControl();
                        var element = control.createElement(screen, this, new Dim2i(x, y + listHeight, width, this.entryHeight).insetLeft(Layout.OPTION_LEFT_INSET), theme);

                        this.addRenderableChild(element);
                        this.controls.add(element);
                        listHeight += this.entryHeight;
                    }
                }

                // scroll up to the start of the mod header if this is the first page of a mod
                var scrollJumpTarget = pageStartY;
                if (modHeaderStart != -1) {
                    scrollJumpTarget = modHeaderStart;
                    modHeaderStart = -1;
                }
                var sectionInfo = new SectionInfo(modOptions, optionPage, pageStartY, listHeight, scrollJumpTarget);
                this.optionToSectionInfo.put(optionPage, sectionInfo);
            }
        }

        return listHeight;
    }

    public void jumpToPage(OptionPage page) {
        var sectionInfo = this.optionToSectionInfo.get(page);
        if (sectionInfo != null) {
            this.ignoreNextScrollUpdate = true;
            this.scrollbar.scrollTo(sectionInfo.scrollJumpTarget);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.enableScissor(this.getX(), this.getY(), this.getLimitX(), this.getLimitY());
        super.render(graphics, mouseX, mouseY, delta);
        graphics.disableScissor();
    }

    private void updateSectionFocus(int scrollAmount) {
        if (this.ignoreNextScrollUpdate) {
            this.ignoreNextScrollUpdate = false;
            return;
        }

        // calculate which y position is considered the "viewed" option,
        // + y is needed to compensate for the initial offset that the .startY values have
        int highlightTarget = scrollAmount + this.getY() + Math.min(this.entryHeight * 3, this.getHeight() / 2);

        // Find which section is currently in the middle of the viewport
        SectionInfo currentSection = null;
        for (SectionInfo section : this.optionToSectionInfo.values()) {
            if (highlightTarget >= section.startY && highlightTarget <= section.endY) {
                currentSection = section;
                break;
            }
        }

        // Only notify if the section has changed
        if (currentSection != null && currentSection != this.lastFocusedSection) {
            this.lastFocusedSection = currentSection;
            this.onPageFocused.accept(currentSection.page());
        }
    }

    private abstract static class HeaderWidget extends AbstractWidget {
        final AbstractOptionList list;
        final String title;
        final int textColor;
        final int backgroundColor;

        public HeaderWidget(AbstractOptionList list, Dim2i dim, String title, int textColor, int backgroundColor) {
            super(dim);
            this.list = list;
            this.title = title;
            this.textColor = textColor;
            this.backgroundColor = backgroundColor;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            this.hovered = this.isMouseOver(mouseX, mouseY);

            this.drawRect(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.backgroundColor);
            this.drawString(graphics, this.truncateLabelToFit(this.title), this.getX() + Layout.OPTION_PAGE_MARGIN, this.getCenterY() + Layout.REGULAR_TEXT_BASELINE_OFFSET, this.textColor);
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
        final ResourceLocation icon;

        public ModHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme, ResourceLocation icon) {
            // super(list, dim, ChatFormatting.UNDERLINE + title, theme.themeLighter, Colors.BACKGROUND_DEFAULT);
            // super(list, dim, ChatFormatting.BOLD + title, theme.themeLighter, ColorARGB.withAlpha(theme.themeDarker, 0x60));
            super(list, dim, ChatFormatting.BOLD + title, theme.themeLighter, Colors.BACKGROUND_DARKER);
            this.icon = icon;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            this.hovered = this.isMouseOver(mouseX, mouseY);

            this.drawRect(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.backgroundColor);

            int textOffset = 0;
            int textY = this.getCenterY() + Layout.REGULAR_TEXT_BASELINE_OFFSET;
            if (this.icon != null) {
                textOffset = VideoSettingsScreen.renderIconWithSpacing(graphics, this.icon, this.textColor, this.getX(), this.getY(), this.getHeight(), Layout.ICON_MARGIN);
                textY = this.getCenterY() + Layout.ICON_TEXT_BASELINE_OFFSET;
            }
            this.drawString(graphics, truncateTextToFit(this.title, this.getWidth() - 12 - textOffset), this.getX() + textOffset, textY, this.textColor);
        }
    }

    private static class PageHeaderWidget extends HeaderWidget {
        public PageHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, "◆ " + title, theme.theme, Colors.BACKGROUND_DEFAULT);
//            super(list, dim, title, theme.themeLighter, ColorARGB.withAlpha(theme.themeDarker, 0x70));
        }
    }

    private static class PageHeaderWidgetInverted extends HeaderWidget {
        public PageHeaderWidgetInverted(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, ChatFormatting.BOLD + title, Colors.FOREGROUND_INVERTED, ColorARGB.withAlpha(theme.theme, 0x70));
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            this.hovered = this.isMouseOver(mouseX, mouseY);

            this.drawRect(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.backgroundColor);
            graphics.drawString(this.font, ChatFormatting.BOLD + this.truncateLabelToFit(this.title), this.getX() + Layout.OPTION_PAGE_MARGIN, this.getCenterY() + Layout.REGULAR_TEXT_BASELINE_OFFSET, this.textColor, false);
        }
    }

    private static class GroupHeaderWidget extends HeaderWidget {
        public GroupHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, ChatFormatting.BOLD + title, Colors.FOREGROUND, Colors.BACKGROUND_MEDIUM);
        }
    }
}
