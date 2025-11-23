package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Builder interface for defining options belonging to a mod and its metadata. A set of mod options contains some list of option pages.
 */
public interface ModOptionsBuilder {
    /**
     * Sets the display name of the mod.
     *
     * @param name The mod name.
     * @return The current builder instance.
     */
    ModOptionsBuilder setName(String name);

    /**
     * Sets the version string of the mod. This value is typically automatically populated from the mod metadata known to the mod loader.
     *
     * @param version The version string.
     * @return The current builder instance.
     */
    ModOptionsBuilder setVersion(String version);

    /**
     * Sets a formatter function for the mod version string. This converts from the raw version string to a display version string.
     *
     * @param versionFormatter The version formatter function.
     * @return The current builder instance.
     */
    ModOptionsBuilder formatVersion(Function<String, String> versionFormatter);

    /**
     * Sets the color theme for the mod options UI.
     *
     * @param colorTheme The color theme builder.
     * @return The current builder instance.
     */
    ModOptionsBuilder setColorTheme(ColorThemeBuilder colorTheme);

    /**
     * Sets the icon texture for the mod. The icon should be centered within the square texture and the background should be transparent. The icon will be rendered monochrome tinted in the mod's theme color.
     *
     * @param texture The resource location of the icon texture.
     * @return The current builder instance.
     */
    ModOptionsBuilder setIcon(ResourceLocation texture);

    /**
     * Sets the icon texture for the mod to be rendered in full color (non-monochrome).
     * 
     * We ask for this to be only in special cases where the icon's design relies on multiple adjacent colors. Typically, the theme color is sufficient for branding.
     *
     * @return The current builder instance.
     */
    ModOptionsBuilder setIconNonMonochrome();

    /**
     * Adds a configuration page to the mod options.
     *
     * @param page The page builder.
     * @return The current builder instance.
     */
    ModOptionsBuilder addPage(PageBuilder page);

    /**
     * Registers an option override provided by this mod. Overrides allow modifying the behavior or appearance of options defined by other mods.
     *
     * @param override The option override builder.
     * @return The current builder instance.
     */
    ModOptionsBuilder registerOptionOverride(OptionOverrideBuilder override);
}
