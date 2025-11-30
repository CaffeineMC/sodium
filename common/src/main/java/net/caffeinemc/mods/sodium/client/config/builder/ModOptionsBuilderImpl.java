package net.caffeinemc.mods.sodium.client.config.builder;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.api.config.structure.*;
import net.caffeinemc.mods.sodium.client.config.structure.*;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

class ModOptionsBuilderImpl implements ModOptionsBuilder {
    private final String configId;
    private String name;
    private String version;
    private ColorTheme theme;
    private ResourceLocation icon;
    private final List<Page> pages = new ArrayList<>();
    private List<OptionOverride> optionOverrides;
    private List<OptionOverlay> optionOverlays;

    ModOptionsBuilderImpl(String configId, String name, String version) {
        this.configId = configId;
        this.name = name;
        this.version = version;
    }

    ModOptions build() {
        Validate.notEmpty(this.name, "Name must not be empty");
        Validate.notEmpty(this.version, "Version must not be empty");

        var overrides = this.optionOverrides == null ? List.<OptionOverride>of() : this.optionOverrides;
        var overlays = this.optionOverlays == null ? List.<OptionOverlay>of() : this.optionOverlays;

        if (this.pages.isEmpty() && overrides.isEmpty() && overlays.isEmpty()) {
            throw new IllegalStateException("At least one page, option override, or option overlay must be added");
        }

        // if no theme is specified, pick one pseudo-randomly based on the configId
        if (this.theme == null) {
            this.theme = ColorTheme.PRESETS[Math.abs(this.configId.hashCode()) % ColorTheme.PRESETS.length];
        }

        return new ModOptions(this.configId, this.name, this.version, this.theme, this.icon, ImmutableList.copyOf(this.pages), overrides, overlays);
    }

    @Override
    public ModOptionsBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public ModOptionsBuilder setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public ModOptionsBuilder formatVersion(Function<String, String> versionFormatter) {
        this.version = versionFormatter.apply(this.version);
        return this;
    }

    @Override
    public ModOptionsBuilder setColorTheme(ColorThemeBuilder theme) {
        this.theme = ((ColorThemeBuilderImpl) theme).build();
        return this;
    }

    @Override
    public ModOptionsBuilder setIcon(ResourceLocation texture) {
        this.icon = texture;
        return this;
    }

    @Override
    public ModOptionsBuilder addPage(PageBuilder builder) {
        this.pages.add(((PageBuilderImpl) builder).build());
        return this;
    }

    @Override
    public ModOptionsBuilder registerOptionReplacement(ResourceLocation target, OptionBuilder replacement) {
        var override = new OptionOverride(target, this.configId, ((OptionBuilderImpl<?>) replacement).build());
        if (this.optionOverrides == null) {
            this.optionOverrides = new ArrayList<>();
        }
        this.optionOverrides.add(override);
        return this;
    }

    @Override
    public ModOptionsBuilder registerOptionOverlay(ResourceLocation target, OptionBuilder overlay) {
        var optionOverlay = new OptionOverlay(target, this.configId, ((OptionBuilderImpl<?>) overlay));
        if (this.optionOverlays == null) {
            this.optionOverlays = new ArrayList<>();
        }
        this.optionOverlays.add(optionOverlay);
        return this;
    }
}
