package net.caffeinemc.mods.sodium.client.config.structure;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.client.config.search.SearchIndex;
import net.caffeinemc.mods.sodium.client.config.search.Searchable;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.minecraft.resources.Identifier;

import java.util.List;

public record ModOptions(String configId, String name, String version, ColorTheme theme, Identifier icon, ImmutableList<Page> pages, List<OptionOverride> overrides, List<OptionOverlay> overlays) implements Searchable {
    @Override
    public void registerTextSources(SearchIndex index) {
        for (Page page : this.pages) {
            page.registerTextSources(index, this);
        }
    }
}
