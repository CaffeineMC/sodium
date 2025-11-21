package net.caffeinemc.mods.sodium.client.gui.widgets;

import net.caffeinemc.mods.sodium.client.config.ConfigManager;
import net.caffeinemc.mods.sodium.client.config.search.SearchQuerySession;
import net.caffeinemc.mods.sodium.client.config.structure.Option;
import net.caffeinemc.mods.sodium.client.gui.Colors;
import net.caffeinemc.mods.sodium.client.gui.Layout;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

public class SearchWidget extends AbstractParentWidget {
    // maximum distance from its original position that a search result can be moved to improve grouping
    private static final int MAX_ORDER_DIST_ERROR = 2;

    private final Consumer<List<Option.OptionNameSource>> onSearchResults;
    private final SearchQuerySession searchQuerySession;
    private String query = "";

    private EditBox searchBox;
    private FlatButtonWidget clearButton;
    private int lastRebuildWidth = -1;

    public SearchWidget(Consumer<List<Option.OptionNameSource>> onSearchResults, Dim2i dim) {
        super(dim);
        this.onSearchResults = onSearchResults;
        this.searchQuerySession = ConfigManager.CONFIG.startSearchQuery();
    }

    public void updateWidgetWidth(int width) {
        if (width != this.lastRebuildWidth) {
            this.lastRebuildWidth = width;
            this.rebuildForWidth(width);
        }
    }

    private void rebuildForWidth(int width) {
        this.clearChildren();

        int x = this.getX();
        int y = this.getY();

        int searchBoxWidth = width - Layout.BUTTON_SHORT;
        this.clearButton = new FlatButtonWidget(
                new Dim2i(x + searchBoxWidth, y, Layout.BUTTON_SHORT, Layout.BUTTON_SHORT),
                Component.literal("×"),
                this::clearSearch,
                true,
                false
        );

        this.searchBox = new EditBox(
                this.font,
                x + Layout.INNER_MARGIN,
                y + Layout.BUTTON_SHORT / 2 - this.font.lineHeight / 2,
                searchBoxWidth - 20,
                Layout.BUTTON_SHORT,
                Component.translatable("sodium.options.search")
        );

        this.searchBox.setMaxLength(200);
        this.searchBox.setBordered(false);
        this.searchBox.setResponder(this::triggerSearch);
        this.searchBox.setHint(Component.translatable("sodium.options.search.hint"));

        this.addChild(this.searchBox);
        this.addChild(this.clearButton);
    }

    private void clearSearch() {
        this.searchBox.setValue("");
        this.query = "";
        this.search();
        this.setFocused(null);
    }

    private void triggerSearch(String text) {
        if (text.equals(this.query)) {
            return;
        }

        this.query = text.stripLeading();
        this.search();
    }

    @SuppressWarnings("unchecked") // we manually check the elements
    private void search() {
        var results = this.searchQuerySession.getSearchResults(this.query);

        // assert assumption of the result type
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            result.setResultIndex(i);

            if (!(result instanceof Option.OptionNameSource)) {
                throw new UnsupportedOperationException("Unsupported search text source type: " + result.getClass().getName());
            }
        }

        List<Option.OptionNameSource> typedResults = (List<Option.OptionNameSource>) results;

        this.improveGrouping(typedResults);
        this.onSearchResults.accept(typedResults);
    }

    private void improveGrouping(List<Option.OptionNameSource> searchResults) {
        // move search results around a little to group them better
        var length = searchResults.size();
        for (int i = 1; i < length - 1; i++) {
            // if the next result would fit better to the previous one than this one, swap current and next
            var prev = searchResults.get(i - 1);
            var curr = searchResults.get(i);
            var next = searchResults.get(i + 1);

            // check that switching current and next doesn't introduce too much of an ordering error
            if (Math.abs(i - prev.getResultIndex()) > MAX_ORDER_DIST_ERROR ||
                    Math.abs(i + 1 - next.getResultIndex()) > MAX_ORDER_DIST_ERROR) {
                continue;
            }

            var prevCurrScore = this.getGroupScore(prev, curr);
            var prevNextScore = this.getGroupScore(prev, next);

            if (prevNextScore > prevCurrScore) {
                searchResults.set(i, next);
                searchResults.set(i + 1, curr);
            }
        }
    }

    private int getGroupScore(Option.OptionNameSource a, Option.OptionNameSource b) {
        if (a.getModOptions() != b.getModOptions()) {
            return 0;
        }
        if (a.getPage() != b.getPage()) {
            return 1;
        }
        if (a.getOptionGroup() != b.getOptionGroup()) {
            return 2;
        }
        return 3;
    }

    @Override
    public void render(@NonNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(this.getX(), this.getY(), this.getX() + this.lastRebuildWidth - Layout.BUTTON_SHORT, this.getLimitY(), Colors.BACKGROUND_DEFAULT);

        this.searchBox.render(graphics, mouseX, mouseY, delta);
        this.clearButton.render(graphics, mouseX, mouseY, delta);

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && this.getFocused() == this.searchBox) {
            this.clearSearch();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return this.searchBox.charTyped(event);
    }

    public boolean isSearching() {
        return this.searchBox.isFocused();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);

        // on focus, focus the search box for typing
        if (focused) {
            this.setFocused(this.searchBox);
        }
    }
}
