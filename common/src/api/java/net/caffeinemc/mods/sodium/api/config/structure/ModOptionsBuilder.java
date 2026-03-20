package net.caffeinemc.mods.sodium.api.config.structure;

import net.caffeinemc.mods.sodium.api.config.ConfigState;
import net.caffeinemc.mods.sodium.api.config.option.FlagHook;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.function.BiConsumer;
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
     * <p>
     * Basic validation is applied to the color theme to ensure it's not grayscale and not too dark.
     *
     * @param colorTheme The color theme builder.
     * @return The current builder instance.
     */
    ModOptionsBuilder setColorTheme(ColorThemeBuilder colorTheme);

    /**
     * Sets the icon texture for the mod. See the documentation for more information about appropriate icon design. The summary is as follows. An icon should:
     *
     * <ul>
     * <li> Be a square texture (e.g. 64x64, 128x128, etc).</li>
     * <li> Have a transparent background unless the main content of the icon fills the entire square.</li>
     * <li> Have binary alpha (fully opaque or fully transparent).</li>
     * <li> Have limited detail since it will be rendered at a small size.</li>
     * </ul>
     * <p>
     * Icons set with this method are tinted monochrome in the mod's theme color. This means the texture itself should be fully white to result in the theme when tinted.
     * <p>
     * No icon will be shown if none is provided and the layout adjusted accordingly.
     *
     * @param texture The ID of the icon texture.
     * @return The current builder instance.
     */
    ModOptionsBuilder setIcon(Identifier texture);

    /**
     * Sets the icon texture for the mod. Same as {@link #setIcon(Identifier)}, but the texture will be rendered in its original color instead of being tinted.
     *
     * @param texture The ID of the icon texture.
     * @return The current builder instance.
     */
    ModOptionsBuilder setNonTintedIcon(Identifier texture);

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

    /**
     * Registers a hook that will be called after an option which has any of the specified flags changed. This can be used to implement custom behavior in response to option changes. To hook on built-in flags, use the identifiers given by {@link OptionFlag#getId()}. The hook is given an array of all flags that triggered the hook. Note that the hook may be called with a set of flags larger than the set of flags it is interested in for performance reasons, since this lets us avoid generating a different flag set for every hook.
     *
     * @param hook     The hook to run, with the set of all triggered flags.
     * @param triggers The flags to listen for.
     * @return The current builder instance.
     */
    ModOptionsBuilder registerFlagHook(BiConsumer<Collection<Identifier>, ConfigState> hook, Identifier... triggers);

    /**
     * Registers a hook just like {@link #registerFlagHook(BiConsumer, Identifier...)}, but using a {@link FlagHook}.
     *
     * @param hook The flag hook to register.
     * @return The current builder instance.
     */
    ModOptionsBuilder registerFlagHook(FlagHook hook);
}
