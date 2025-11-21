package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

public interface ConfigBuilder {
    ModOptionsBuilder registerModOptions(String namespace, String name, String version);

    ModOptionsBuilder registerModOptions(String namespace);

    ModOptionsBuilder registerOwnModOptions();

    OptionOverrideBuilder createOptionOverride();

    ColorThemeBuilder createColorTheme();

    OptionPageBuilder createOptionPage();

    ExternalPageBuilder createExternalPage();

    OptionGroupBuilder createOptionGroup();

    BooleanOptionBuilder createBooleanOption(Identifier id);

    IntegerOptionBuilder createIntegerOption(Identifier id);

    <E extends Enum<E>> EnumOptionBuilder<E> createEnumOption(Identifier id, Class<E> enumClass);

    ExternalButtonOptionBuilder createExternalButtonOption(Identifier id);
}
