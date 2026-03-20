package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.ColorThemeBuilder;
import net.caffeinemc.mods.sodium.client.gui.ColorTheme;
import net.caffeinemc.mods.sodium.client.gui.Colors;

public class ColorThemeBuilderImpl implements ColorThemeBuilder {
    private static final float MIN_THEME_SATURATION = 0.2f;
    private static final float MIN_THEME_BRIGHTNESS = 0.55f;

    private int baseTheme;
    private int themeHighlight;
    private int themeDisabled;

    ColorTheme build() {
        if (this.baseTheme == 0) {
            throw new IllegalStateException("Base theme must be set");
        }

        this.baseTheme = Colors.constrainColorHSV(this.baseTheme, MIN_THEME_SATURATION, MIN_THEME_BRIGHTNESS);

        if (this.themeHighlight == 0 || this.themeDisabled == 0) {
            return new ColorTheme(this.baseTheme);
        } else {
            this.themeHighlight = Colors.constrainColorHSV(this.themeHighlight, MIN_THEME_SATURATION, MIN_THEME_BRIGHTNESS);
            this.themeDisabled = Colors.constrainColorHSV(this.themeDisabled, MIN_THEME_SATURATION, 0);
            return new ColorTheme(this.baseTheme, this.themeHighlight, this.themeDisabled);
        }
    }

    @Override
    public ColorThemeBuilder setBaseThemeRGB(int theme) {
        this.baseTheme = theme | 0xFF000000;
        return this;
    }

    @Override
    public ColorThemeBuilder setFullThemeRGB(int theme, int themeHighlight, int themeDisabled) {
        this.baseTheme = theme | 0xFF000000;
        this.themeHighlight = themeHighlight | 0xFF000000;
        this.themeDisabled = themeDisabled | 0xFF000000;
        return this;
    }
}
