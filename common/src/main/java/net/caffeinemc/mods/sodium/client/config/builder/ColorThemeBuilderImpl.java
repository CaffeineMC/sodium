package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.ColorThemeBuilder;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;

public class ColorThemeBuilderImpl implements ColorThemeBuilder {
    private int baseTheme;
    private int themeHighlight;
    private int themeDisabled;

    ColorTheme build() {
        if (this.baseTheme == 0) {
            throw new IllegalStateException("Base theme must be set");
        }

        if (this.themeHighlight == 0 || this.themeDisabled == 0) {
            return new ColorTheme(this.baseTheme);
        } else {
            return new ColorTheme(this.baseTheme, this.themeHighlight, this.themeDisabled);
        }
    }

    @Override
    public ColorThemeBuilder setBaseThemeRGB(int theme) {
        this.baseTheme = theme & 0xFFFFFF;
        return this;
    }

    @Override
    public ColorThemeBuilder setFullThemeRGB(int theme, int themeHighlight, int themeDisabled) {
        this.baseTheme = theme & 0xFFFFFF;
        this.themeHighlight = themeHighlight & 0xFFFFFF;
        this.themeDisabled = themeDisabled & 0xFFFFFF;
        return this;
    }
}
