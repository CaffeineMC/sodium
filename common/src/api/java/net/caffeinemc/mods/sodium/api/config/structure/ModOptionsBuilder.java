package net.caffeinemc.mods.sodium.api.config.structure;

import net.minecraft.resources.Identifier;

import java.util.function.Function;

public interface ModOptionsBuilder {
    ModOptionsBuilder setName(String name);

    ModOptionsBuilder setVersion(String version);

    ModOptionsBuilder formatVersion(Function<String, String> versionFormatter);

    ModOptionsBuilder setColorTheme(ColorThemeBuilder colorTheme);
    
    ModOptionsBuilder setIcon(Identifier texture);

    ModOptionsBuilder addPage(PageBuilder page);

    ModOptionsBuilder registerOptionOverride(OptionOverrideBuilder override);
}
