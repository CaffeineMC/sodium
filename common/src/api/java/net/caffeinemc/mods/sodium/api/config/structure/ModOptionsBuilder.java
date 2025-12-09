package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

import java.util.function.Function;

/**
 * Builder interface for defining options belonging to a mod and its metadata. A set of mod options contains some list of option pages, option overrides, and option overlays. At least one page, override, or overlay must be defined.
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
     * Sets the color theme for the mod options UI. A color theme is optional and a theme will be chosen at random (deterministically) from a predetermined set of reasonable colors if none is provided.
     *
     * @param colorTheme The color theme builder.
     * @return The current builder instance.
     */
    ModOptionsBuilder setColorTheme(ColorThemeBuilder colorTheme);

    /**
     * Sets the icon texture for the mod. The icon should be centered within the square texture and the background should be transparent. The icon will be rendered monochrome tinted in the mod's theme color. No icon will be shown if none is provided and the layout adjusted accordingly.
     *
     * @param texture The resource location of the icon texture.
     * @return The current builder instance.
     */
    ModOptionsBuilder setIcon(Identifier texture);

    /**
     * Adds a configuration page to the mod options.
     *
     * @param page The page builder.
     * @return The current builder instance.
     */
    ModOptionsBuilder addPage(PageBuilder page);

    /**
     * Registers an option override provided by this mod. Overrides allow modifying the behavior or appearance of options defined by other mods.
     * <p>
     * The ID of the provided replacement option can match the original option to allow other mods to apply overlays targeting the original option ID. If the replacement option has a different ID, overlays must target the new ID.
     *
     * @param target      The ID of the option to override.
     * @param replacement The option builder that defines the replacement option.
     * @return The current builder instance.
     */
    ModOptionsBuilder registerOptionReplacement(Identifier target, OptionBuilder replacement);

    /**
     * Registers an option overlay provided by this mod. Overlays allow partially changing an option instead of replacing it entirely.
     * <p>
     * The target option ID must match the ID of an existing option, either defined by another mod or by a replacement option defined by this mod. If the target option has been replaced, overlays must target the ID of the replacement option, which may or may not be the same as the original option ID.
     *
     * @param target  The ID of the option to overlay.
     * @param overlay The option builder that defines the overlay changes.
     * @return The current builder instance.
     */
    ModOptionsBuilder registerOptionOverlay(Identifier target, OptionBuilder overlay);
}
