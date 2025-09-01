package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
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
    private int entryHeight;

    public record SectionInfo(ModOptions modOptions, OptionPage page, int startY, int endY) {
    }

    // Constructor for showing all pages
    public OptionListWidget(Screen screen, Dim2i dim) {
        super(dim.insetLeft(Layout.OPTION_GROUP_MARGIN));
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
        this.scrollbar = this.addRenderableChild(new ScrollbarWidget(new Dim2i(x + width + Layout.OPTION_LIST_SCROLLBAR_OFFSET, y, Layout.SCROLLBAR_WIDTH, height), this::updateSectionFocus));

        this.entryHeight = this.font.lineHeight * 2;
        int listHeight;

        if (this.filteredOptions != null) {
            listHeight = this.renderFilteredOptions(screen, x, y, width);
        } else {
            listHeight = this.renderAllPages(screen, x, y, width);
        }

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
            else if (lastSource.getOptionGroup() != source.getOptionGroup()) {
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

    private void updateSectionFocus(int scrollAmount) {
        if (this.onSectionFocused == null || !this.showAllPages) {
            return;
        }

        // calculate which y position is considered the "viewed" option,
        // + y is needed to compensate for the initial offset that the .startY values have
        int highlightTarget = scrollAmount + this.getY() + Math.min(this.entryHeight * 3, this.getHeight() / 2);

        // Find which section is currently in the middle of the viewport
        SectionInfo currentSection = null;
        for (SectionInfo section : this.sectionInfoMap.values()) {
            if (highlightTarget >= section.startY && highlightTarget <= section.endY) {
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
            this.drawString(graphics, this.truncateLabelToFit(this.title), this.getX() + Layout.OPTION_PAGE_MARGIN, this.getCenterY() - 4, this.textColor);
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
        private static final int ICON_MARGIN = 3;
        
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

            int iconSize = 0, textOffset = 0;
            if (this.icon != null) {
                iconSize = this.getHeight() - ICON_MARGIN * 2;
                textOffset = ICON_MARGIN + iconSize - 1;
            }
            
            this.drawRect(graphics, this.getX(), this.getY(), this.getLimitX(), this.getLimitY(), this.backgroundColor);

            this.drawString(graphics, truncateTextToFit(this.title, this.getWidth() - 12 - textOffset), this.getX() + Layout.OPTION_PAGE_MARGIN + textOffset, this.getCenterY() - 4, this.textColor);
            
            // render the icon if available
            if (this.icon == null) {
                return;
            }

            var texture = Minecraft.getInstance().getTextureManager().getTexture(this.icon);
            int w = texture.getTexture().getWidth(0);
            int h = texture.getTexture().getHeight(0);

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon, this.getX() + ICON_MARGIN, this.getCenterY() - iconSize / 2, 0, 0, iconSize, iconSize, w, h, w, h);
        }
    }

    private static class PageHeaderWidget extends HeaderWidget {
        public PageHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, ChatFormatting.BOLD + title, theme.theme, Colors.BACKGROUND_DEFAULT);
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
            graphics.drawString(this.font, ChatFormatting.BOLD + this.truncateLabelToFit(this.title), this.getX() + Layout.OPTION_PAGE_MARGIN, this.getCenterY() - 4, this.textColor, false);
        }
    }

    private static class GroupHeaderWidget extends HeaderWidget {
        public GroupHeaderWidget(AbstractOptionList list, Dim2i dim, String title, ColorTheme theme) {
            super(list, dim, ChatFormatting.BOLD + title, Colors.FOREGROUND, Colors.BACKGROUND_MEDIUM);
        }
    }
}
