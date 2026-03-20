package net.caffeinemc.mods.sodium.api.config.structure;

/**
 * Builder interface for defining color themes.
 * 
 * Colors use RGB integers (bits 0 to 24). This is ARGB format with alpha bits ignored.
 */
public interface ColorThemeBuilder {
    /**
     * Sets the base theme color.
     *
     * @param theme Theme color as an RGB integer.
     * @return The current builder instance.
     */
    ColorThemeBuilder setBaseThemeRGB(int theme);

    /**
     * Sets the full theme colors.
     *
     * @param theme          Theme color as an RGB integer.
     * @param themeHighlight Theme highlight color as an RGB integer.
     * @param themeDisabled  Theme disabled color as an RGB integer.
     * @return The current builder instance.
     */
    ColorThemeBuilder setFullThemeRGB(int theme, int themeHighlight, int themeDisabled);
}
