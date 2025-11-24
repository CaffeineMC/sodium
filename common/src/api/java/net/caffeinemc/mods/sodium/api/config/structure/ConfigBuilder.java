package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.ResourceLocation;

/**
 * Root builder interface for defining configuration structures. An implementation of this class is passed to your entry point and lets you create builders for everything in the API. Builders do not need to be built manually, as they are automatically closed when the enclosing scope ends.
 * <p>
 * Refer to USAGE.md for usage instructions.
 */
public interface ConfigBuilder {
    /**
     * Registers a new mod options structure with the given configId, name, and version. Note that a mod may register multiple mod options structures under different configIds if desired.
     *
     * @param configId The configId of these mod options.
     * @param name     The human-readable name of the mod.
     * @param version  The version of the mod.
     * @return A builder for defining the mod's configuration structure.
     */
    ModOptionsBuilder registerModOptions(String configId, String name, String version);

    /**
     * Registers a new mod options structure for the mod with the given configId. The mod's name and version will be looked up automatically.
     *
     * @param namespace The configId of these mod options.
     * @return A builder for defining the mod's configuration structure.
     */
    ModOptionsBuilder registerModOptions(String namespace);

    /**
     * Registers a new mod options structure for the mod of the current entrypoint. The mod's name and version will be looked up automatically.
     *
     * @return A builder for defining the mod's configuration structure.
     */
    ModOptionsBuilder registerOwnModOptions();

    /**
     * Creates a new option override builder.
     *
     * @return A builder for defining an option override.
     */
    OptionOverrideBuilder createOptionOverride();

    /**
     * Creates a new color theme builder.
     *
     * @return A builder for defining a color theme.
     */
    ColorThemeBuilder createColorTheme();

    /**
     * Creates a new option page builder.
     *
     * @return A builder for defining an option page.
     */
    OptionPageBuilder createOptionPage();

    /**
     * Creates a new external page builder.
     *
     * @return A builder for defining an external page.
     */
    ExternalPageBuilder createExternalPage();

    /**
     * Creates a new option group builder.
     *
     * @return A builder for defining an option group.
     */
    OptionGroupBuilder createOptionGroup();

    /**
     * Creates a new boolean option builder.
     *
     * @param id The unique identifier for this option.
     * @return A builder for defining a boolean option.
     */
    BooleanOptionBuilder createBooleanOption(ResourceLocation id);

    /**
     * Creates a new integer option builder.
     *
     * @param id The unique identifier for this option.
     * @return A builder for defining an integer option.
     */
    IntegerOptionBuilder createIntegerOption(ResourceLocation id);

    /**
     * Creates a new enum option builder.
     *
     * @param id        The unique identifier for this option.
     * @param enumClass The enum class for this option.
     * @param <E>       The enum type.
     * @return A builder for defining an enum option.
     */
    <E extends Enum<E>> EnumOptionBuilder<E> createEnumOption(ResourceLocation id, Class<E> enumClass);

    /**
     * Creates a new external button option builder.
     *
     * @param id The unique identifier for this option.
     * @return A builder for defining an external button option.
     */
    ExternalButtonOptionBuilder createExternalButtonOption(ResourceLocation id);
}
