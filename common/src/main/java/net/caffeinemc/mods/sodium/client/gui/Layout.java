package net.caffeinemc.mods.sodium.client.gui;

import net.minecraft.client.gui.Font;

public class Layout {
    public static final int BUTTON_SHORT = 20;
    public static final int BUTTON_LONG = 65;
    public static final int INNER_MARGIN = 5;

    public static final int OPTION_GROUP_MARGIN = 3;
    public static final int OPTION_PAGE_MARGIN = 6;
    public static final int OPTION_MOD_MARGIN = 12;
    public static final int OPTION_LEFT_INSET = OPTION_GROUP_MARGIN;

    public static final int SCROLLBAR_WIDTH = 7;

    public static final int TEXT_LEFT_PADDING = 8;
    public static final int TEXT_PARAGRAPH_SPACING = 8;
    public static final int TEXT_LINE_SPACING = 2;
    public static final int REGULAR_TEXT_BASELINE_OFFSET = -4;

    public static final int PAGE_LIST_WIDTH = 125;

    public static final int OPTION_WIDTH = 210;
    public static final int OPTION_LIST_SCROLLBAR_OFFSET = 5;
    public static final int OPTION_TEXT_SIDE_PADDING = 6;
    public static final int OPTION_LABEL_END_PADDING = 20;

    public static final int TICKBOX_CONTROL_WIDTH = 30;
    public static final int CYCLING_CONTROL_WIDTH = 70;
    public static final int SLIDER_WIDTH = 90;
    public static final int SLIDER_HEIGHT = 10;

    public static final int ICON_MARGIN = 4;
    public static final int ICON_TEXT_BASELINE_OFFSET = -3;
    public static final int CONTROL_ICON_SIZE = 10;

    public static final int MIN_TOOLTIP_WIDTH = 100;
    public static final int MAX_TOOLTIP_WIDTH = 200;
    public static final int TOOLTIP_OUTER_MARGIN = 3;

    public static final int CONTENT_BORDER_MIN_WIDTH = 100;
    public static final int CONTENT_BORDER_HEIGHT = OPTION_MOD_MARGIN;
    public static final int CONTENT_MIN_HEIGHT = 300;

    public static final int PAGE_ENTRY_SELECTION_BAR_WIDTH = 3;
    public static final int PAGE_ENTRY_LABEL_END_PADDING = 14;

    // number of rows below the viewport top used as the highlight target when scrolling
    public static final int SECTION_FOCUS_LEAD_ROWS = 3;

    // height of an option row or page-list entry: two lines of text tall
    public static int entryHeight(Font font) {
        return font.lineHeight * 2;
    }

    // height of a mod header in the page list: three lines of text tall
    public static int pageHeaderHeight(Font font) {
        return font.lineHeight * 3;
    }
}
