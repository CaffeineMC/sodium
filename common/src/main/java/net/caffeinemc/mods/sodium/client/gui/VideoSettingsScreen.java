package net.caffeinemc.mods.sodium.client.gui;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.structure.IntegerOption;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.caffeinemc.mods.sodium.client.config.structure.Page;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlElement;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPrompt;
import net.caffeinemc.mods.sodium.client.gui.prompt.ScreenPromptable;
import net.caffeinemc.mods.sodium.client.gui.screen.ConfigCorruptedScreen;
import net.caffeinemc.mods.sodium.client.gui.widgets.*;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class VideoSettingsScreen extends Screen implements ScreenPromptable, ScrollableTooltip.TooltipParent {
    private final Screen prevScreen;
    private final @Nullable OptionPage initiallyFocusedPage;

    private Dim2i dim;
    private boolean insetX, insetY;

    private PageListWidget pageList;
    private SearchWidget searchWidget;
    private OptionListWidget optionList;

    private FlatButtonWidget applyButton, closeButton, undoButton;
    private DonationButtonWidget donateButton;

    private boolean hasPendingChanges;

    private final ScrollableTooltip tooltip = new ScrollableTooltip(this);

    private @Nullable ScreenPrompt prompt;

    private VideoSettingsScreen(Screen prevScreen) {
        this(prevScreen, null);
    }

    private VideoSettingsScreen(Screen prevScreen, @Nullable OptionPage initiallyFocusedPage) {
        super(Component.literal("Sodium Renderer Settings"));

        this.prevScreen = prevScreen;
        this.initiallyFocusedPage = initiallyFocusedPage;

        this.checkPromptTimers();

        // the binding values may have been modified in the meantime, reload from binding to update
        ConfigManager.CONFIG.resetAllOptionsFromBindings();
    }

    private void checkPromptTimers() {
        // Never show the prompt in developer workspaces.
        if (PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        var options = SodiumClientMod.options();

        // If the user has already seen the prompt, don't show it again.
        if (options.notifications.hasSeenDonationPrompt) {
            return;
        }

        HashedFingerprint fingerprint = null;

        try {
            fingerprint = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            SodiumClientMod.logger()
                    .error("Failed to read the fingerprint from disk", t);
        }

        // If the fingerprint doesn't exist, or failed to be loaded, abort.
        if (fingerprint == null) {
            return;
        }

        // The fingerprint records the installation time. If it's been a while since installation, show the user
        // a prompt asking for them to consider donating.
        var now = Instant.now();
        var threshold = Instant.ofEpochSecond(fingerprint.timestamp())
                .plus(3, ChronoUnit.DAYS);

        if (now.isAfter(threshold)) {
            this.openDonationPrompt(options);
        }
    }

    private void openDonationPrompt(SodiumOptions options) {
        var prompt = new ScreenPrompt(this, DONATION_PROMPT_MESSAGE, 320, 190,
                new ScreenPrompt.Action(Component.literal("Buy us a coffee"), this::openDonationPage));
        prompt.setFocused(true);

        options.notifications.hasSeenDonationPrompt = true;

        try {
            SodiumOptions.writeToDisk(options);
        } catch (IOException e) {
            SodiumClientMod.logger()
                    .error("Failed to update config file", e);
        }
    }

    public static Screen createScreen(Screen currentScreen) {
        return createScreen(currentScreen, null);
    }

    public static Screen createScreen(Screen currentScreen, @Nullable OptionPage initiallyFocusedPage) {
        if (SodiumClientMod.options().isReadOnly()) {
            return new ConfigCorruptedScreen(currentScreen, VideoSettingsScreen::new);
        } else {
            return new VideoSettingsScreen(currentScreen, initiallyFocusedPage);
        }
    }

    @Override
    protected void init() {
        super.init();

        ConfigManager.CONFIG.invalidateGlobalRebuildDependents();
        this.rebuild();

        if (this.prompt != null) {
            this.prompt.init();
        }

        if (this.initiallyFocusedPage != null) {
            this.jumpToPage(this.initiallyFocusedPage);
            this.onSectionFocused(this.initiallyFocusedPage);
        }
    }

    private int ifInsetX(int value) {
        return this.insetX ? value : 0;
    }

    private int ifInsetY(int value) {
        return this.insetY ? value : 0;
    }

    private int ifNotInsetX(int value) {
        return this.insetX ? 0 : value;
    }

    private int ifNotInsetY(int value) {
        return this.insetY ? 0 : value;
    }

    private void rebuild() {
        this.clearWidgets();

        this.updateScreenDimensions();
        var x = this.getX();
        var y = this.getY();
        var w = this.getWidth();
        var h = this.getHeight();

        int topBarHeight = Layout.BUTTON_SHORT;
        this.searchWidget = new SearchWidget(this::onSearchResults, new Dim2i(x, y, w, topBarHeight));

        int topBarClear = topBarHeight + ifInsetY(Layout.INNER_MARGIN);
        this.pageList = new PageListWidget(new Dim2i(x, y + topBarClear, Layout.PAGE_LIST_WIDTH, h - topBarClear), this);
        this.addRenderableWidget(this.pageList);

        boolean stackVertically = false;
        boolean reserveBottomSpace = false;

        int minWidthToStack = Layout.PAGE_LIST_WIDTH + Layout.INNER_MARGIN * 2 + Layout.OPTION_WIDTH + Layout.OPTION_LIST_SCROLLBAR_OFFSET + Layout.SCROLLBAR_WIDTH + Layout.BUTTON_LONG;
        int maxWidthToStack = minWidthToStack + Layout.BUTTON_LONG * 2 + Layout.INNER_MARGIN;

        if (w > minWidthToStack && w < maxWidthToStack) {
            stackVertically = true;
        } else if (w < minWidthToStack) {
            reserveBottomSpace = true;
        }

        this.closeButton = new FlatButtonWidget(new Dim2i(this.getLimitX() - Layout.BUTTON_LONG - ifNotInsetX(Layout.INNER_MARGIN), this.getLimitY() - (ifNotInsetY(Layout.INNER_MARGIN) + Layout.BUTTON_SHORT), Layout.BUTTON_LONG, Layout.BUTTON_SHORT), Component.translatable("gui.done"), this::onClose, true, false);
        this.addRenderableWidget(this.closeButton);

        if (stackVertically) {
            this.applyButton = new FlatButtonWidget(new Dim2i(this.closeButton.getX(), this.closeButton.getY() - (Layout.INNER_MARGIN + Layout.BUTTON_SHORT), Layout.BUTTON_LONG, Layout.BUTTON_SHORT), Component.translatable("sodium.options.buttons.apply"), ConfigManager.CONFIG::applyAllOptions, true, false);
            this.undoButton = new FlatButtonWidget(new Dim2i(this.applyButton.getX(), this.applyButton.getY() - (Layout.INNER_MARGIN + Layout.BUTTON_SHORT), Layout.BUTTON_LONG, Layout.BUTTON_SHORT), Component.translatable("sodium.options.buttons.undo"), this::undoChanges, true, false);
        } else {
            this.applyButton = new FlatButtonWidget(new Dim2i(this.closeButton.getX() - Layout.INNER_MARGIN - Layout.BUTTON_LONG, this.getLimitY() - (Layout.INNER_MARGIN + Layout.BUTTON_SHORT), Layout.BUTTON_LONG, Layout.BUTTON_SHORT), Component.translatable("sodium.options.buttons.apply"), ConfigManager.CONFIG::applyAllOptions, true, false);
            this.undoButton = new FlatButtonWidget(new Dim2i(this.applyButton.getX() - Layout.INNER_MARGIN - Layout.BUTTON_LONG, this.getLimitY() - (Layout.INNER_MARGIN + Layout.BUTTON_SHORT), Layout.BUTTON_LONG, Layout.BUTTON_SHORT), Component.translatable("sodium.options.buttons.undo"), this::undoChanges, true, false);
        }
        this.addRenderableWidget(this.undoButton);
        this.addRenderableWidget(this.applyButton);

        this.donateButton = new DonationButtonWidget(this, this::openDonationPage, this::hideDonationButton);
        this.addRenderableWidget(this.searchWidget);
        this.updateSearchWidgetWidth();

        var optionListDim = new Dim2i(
                this.pageList.getLimitX(),
                y + topBarHeight + Layout.INNER_MARGIN,
                Layout.OPTION_WIDTH + Layout.OPTION_LIST_SCROLLBAR_OFFSET + Layout.SCROLLBAR_WIDTH,
                h - topBarHeight - (reserveBottomSpace ? (Layout.INNER_MARGIN * 2 + Layout.BUTTON_SHORT) : Layout.INNER_MARGIN) - ifNotInsetY(Layout.INNER_MARGIN)
        );
        this.optionList = new OptionListWidget(this, optionListDim, this::onSectionFocused);
        this.addRenderableWidget(this.optionList);

        var tooltipAreaY = y + topBarHeight + ifInsetY(Layout.TOOLTIP_OUTER_MARGIN);
        this.tooltip.setTooltipArea(
                new Dim2i(
                        this.optionList.getLimitX(),
                        tooltipAreaY,
                        this.getLimitX() - this.optionList.getLimitX() - ifNotInsetX(Layout.TOOLTIP_OUTER_MARGIN),
                        this.getLimitY() - tooltipAreaY - ifNotInsetY(Layout.TOOLTIP_OUTER_MARGIN)
                )
        );
    }

    private void updateScreenDimensions() {
        // size screen to not be too wide
        var baseContentWidth = Layout.PAGE_LIST_WIDTH + Layout.INNER_MARGIN + Layout.OPTION_WIDTH + Layout.OPTION_LIST_SCROLLBAR_OFFSET + Layout.SCROLLBAR_WIDTH + Layout.TOOLTIP_OUTER_MARGIN;
        var minContentWidth = baseContentWidth + (Layout.MAX_TOOLTIP_WIDTH - Layout.MIN_TOOLTIP_WIDTH) / 2 + Layout.MIN_TOOLTIP_WIDTH;
        var maxContentWidth = baseContentWidth + Layout.MAX_TOOLTIP_WIDTH;
        var maxInterpolatingBorderWidth = 100;
        var widthInterpolationStart = minContentWidth + Layout.CONTENT_BORDER_MIN_WIDTH;
        var widthInterpolationEnd = maxContentWidth + maxInterpolatingBorderWidth;

        int contentWidth = this.width;
        this.insetX = false;
        if (this.width > minContentWidth + Layout.CONTENT_BORDER_MIN_WIDTH) {
            // interpolate between min and max content width based on current width
            if (this.width < widthInterpolationEnd) {
                float t = (float) (this.width - widthInterpolationStart) / (widthInterpolationEnd - widthInterpolationStart);
                contentWidth = minContentWidth + (int) (t * (maxContentWidth - minContentWidth));
            } else {
                contentWidth = maxContentWidth;
            }
            this.insetX = true;
        }

        // for height, it's the other way around. there's a maximum border height
        int contentHeight = this.height;
        this.insetY = false;
        if (this.height > Layout.CONTENT_MIN_HEIGHT + Layout.CONTENT_BORDER_HEIGHT && this.insetX) {
            contentHeight = this.height - Layout.CONTENT_BORDER_HEIGHT;
            this.insetY = true;
        }

        // center the content area
        this.dim = new Dim2i(
                (this.width - contentWidth) / 2,
                (this.height - contentHeight) / 2,
                contentWidth,
                contentHeight
        );
    }

    private void onSearchResults(List<Option.OptionNameSource> searchResults) {
        if (searchResults.isEmpty()) {
            this.optionList.clearFilter();
        } else {
            this.optionList.setFilteredOptions(searchResults);
        }
        this.optionList.rebuild(this);
    }

    private void onSectionFocused(Page page) {
        this.pageList.switchSelected(page);
    }

    public void jumpToPage(Page page) {
        if (this.optionList != null) {
            this.optionList.jumpToPage(page);
        }
    }

    private void updateSearchWidgetWidth() {
        this.searchWidget.updateWidgetWidth(this.getWidth() - this.donateButton.getWidth());
    }

    private void hideDonationButton() {
        SodiumOptions options = SodiumClientMod.options();
        options.notifications.hasClearedDonationButton = true;

        try {
            SodiumOptions.writeToDisk(options);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save configuration", e);
        }

        this.donateButton.updateDisplay(this, false);
        this.updateSearchWidgetWidth();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        this.updateControls(mouseX, mouseY);

        super.extractRenderState(graphics, this.prompt != null ? -1 : mouseX, this.prompt != null ? -1 : mouseY, delta);

        if (this.prompt != null) {
            this.prompt.extractRenderState(graphics, mouseX, mouseY, delta);
        } else {
            this.tooltip.render(graphics);
        }
    }

    private void updateControls(int mouseX, int mouseY) {
        boolean hasChanges = ConfigManager.CONFIG.anyOptionChanged();

        this.applyButton.setEnabled(hasChanges);
        this.undoButton.setVisible(hasChanges);
        this.closeButton.setEnabled(!hasChanges);

        AbstractWidget reservedAreaBlocker;
        if (hasChanges) {
            reservedAreaBlocker = this.undoButton;
        } else {
            reservedAreaBlocker = this.applyButton;
        }
        this.tooltip.setReservedAreaTopLeftCorner(reservedAreaBlocker.getX(), reservedAreaBlocker.getY());

        this.hasPendingChanges = hasChanges;

        // determine the tooltip hover target
        // this is the first item that's hovered over, or if nothing is hovered, the focused item
        ControlElement hovered = null;
        ControlElement focused = null;
        if (mouseX >= this.optionList.getX() && mouseX <= this.optionList.getLimitX() &&
                mouseY >= this.optionList.getY() && mouseY <= this.optionList.getLimitY()) {
            for (ControlElement element : this.optionList.getControls()) {
                if (element.isMouseOver(mouseX, mouseY)) {
                    hovered = element;
                    break;
                }
                if (element.isFocused()) {
                    focused = element;
                }
            }
        }
        var hoverTarget = hovered != null ? hovered : focused;

        this.tooltip.onControlHover(hoverTarget, mouseX, mouseY);
    }

    private void undoChanges() {
        ConfigManager.CONFIG.resetAllOptionsFromBindings();
    }

    private void openDonationPage() {
        Util.getPlatform().openUri("https://caffeinemc.net/donate");
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.prompt != null && this.prompt.keyPressed(event)) {
            return true;
        }

        if (this.searchWidget.keyPressed(event)) {
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (this.prompt == null && !this.searchWidget.isSearching()) {
            // shift + P opens the vanilla video settings screen
            if (event.key() == GLFW.GLFW_KEY_P && (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) {
                Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.options.VideoSettingsScreen(this.prevScreen, Minecraft.getInstance(), Minecraft.getInstance().options));
                return true;
            }

            // T starts search
            if (event.key() == GLFW.GLFW_KEY_T) {
                this.setFocused(this.searchWidget);
                return true;
            }
        }

        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.prompt != null) {
            return this.prompt.mouseClicked(event, doubleClick);
        }

        if (!super.mouseClicked(event, doubleClick)) {
            // Clicking in empty space, focus the search bar
            if (!this.searchWidget.isFocused()) {
                this.setFocused(this.searchWidget);
                return true;
            }
            this.setFocused(null);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double f, double amount) {
        // change the gui scale with scrolling if the control key is held
        if (Minecraft.getInstance().hasControlDown()) {
            var location = Identifier.parse("sodium:general.gui_scale");
            var option = ConfigManager.CONFIG.getOption(location);
            if (option instanceof IntegerOption guiScaleOption) {
                if (guiScaleOption.getValidatedValue() instanceof Integer intValue) {
                    var range = guiScaleOption.getSteppedValidator();
                    var top = range.max() + 1;
                    var auto = range.min();

                    // re-maps the auto value (presumably 0) to be at the top of the scroll range
                    if (intValue == auto) {
                        intValue = top;
                    }
                    var newValue = Math.clamp(intValue + (int) Math.signum(amount), auto + 1, top);
                    if (newValue != intValue) {
                        if (newValue == top) {
                            newValue = auto;
                        }
                        if (range.isValueValid(newValue)) {
                            guiScaleOption.modifyValue(newValue);
                            ConfigManager.CONFIG.applyOption(location);
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        if (this.tooltip.mouseScrolled(x, y, amount)) {
            return true;
        }

        return super.mouseScrolled(x, y, f, amount);
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T guiEventListener) {
        return super.addRenderableWidget(guiEventListener);
    }

    @Override
    public void removeWidget(GuiEventListener guiEventListener) {
        super.removeWidget(guiEventListener);
    }

    public <T extends GuiEventListener & Renderable & NarratableEntry> void setWidgetPresence(T guiEventListener, boolean present) {
        this.removeWidget(guiEventListener);
        if (present) {
            this.addRenderableWidget(guiEventListener);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !this.hasPendingChanges;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.prevScreen);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.prompt == null ? super.children() : this.prompt.getWidgets();
    }

    @Override
    public void setPrompt(@Nullable ScreenPrompt prompt) {
        this.prompt = prompt;
    }

    @Nullable
    @Override
    public ScreenPrompt getPrompt() {
        return this.prompt;
    }

    @Override
    public Dim2i getDimensions() {
        return this.dim;
    }

    public static int renderIconWithSpacing(GuiGraphicsExtractor graphics, Identifier icon, int color, boolean iconMonochrome, int x, int y, int height, int margin) {
        int iconSize = height - margin * 2;

        var texture = Minecraft.getInstance().getTextureManager().getTexture(icon);
        int w = texture.getTexture().getWidth(0);
        int h = texture.getTexture().getHeight(0);

        x = x + margin;
        y = y + height / 2 - iconSize / 2;
        if (iconMonochrome) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, iconSize, iconSize, w, h, w, h, color);
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, iconSize, iconSize, w, h, w, h);
        }

        return margin * 2 + iconSize;
    }

    private static final List<FormattedText> DONATION_PROMPT_MESSAGE;

    static {
        DONATION_PROMPT_MESSAGE = List.of(
                FormattedText.composite(Component.literal("Hello!")),
                FormattedText.composite(Component.literal("It seems that you've been enjoying "), Component.literal("Sodium").withColor(0x27eb92), Component.literal(", the powerful and open rendering optimization mod for Minecraft.")),
                FormattedText.composite(Component.literal("Mods like these are complex. They require "), Component.literal("thousands of hours").withColor(0xff6e00), Component.literal(" of development, debugging, and tuning to create the experience that players have come to expect.")),
                FormattedText.composite(Component.literal("If you'd like to show your token of appreciation, and support the development of our mod in the process, then consider "), Component.literal("buying us a coffee").withColor(0xed49ce), Component.literal(".")),
                FormattedText.composite(Component.literal("And thanks again for using our mod! We hope it helps you (and your computer.)"))
        );
    }
}
