package net.caffeinemc.mods.sodium.client.config.builder;

import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.StaticOption;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.Validate;

abstract class StaticOptionBuilderImpl<O extends StaticOption> extends OptionBuilderImpl<O> {
    private Component tooltip;

    StaticOptionBuilderImpl(Identifier id) {
        super(id);
    }

    @Override
    void validateData() {
        Validate.notNull(this.getTooltip(), "Tooltip must be set");
    }

    Component getTooltip() {
        return getFirstNotNull(this.tooltip, StaticOption::getTooltip);
    }

    @Override
    public OptionBuilder setTooltip(Component tooltip) {
        Validate.notNull(tooltip, "Argument must not be null");

        this.tooltip = tooltip;
        return this;
    }
}
