package net.caffeinemc.mods.sodium.client.config.builder;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.api.config.structure.OptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionGroupBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;
import net.caffeinemc.mods.sodium.client.config.structure.OptionGroup;
import net.caffeinemc.mods.sodium.client.config.structure.OptionPage;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;

class OptionPageBuilderImpl extends PageBuilderImpl implements OptionPageBuilder {
    private final List<OptionGroup> groups = new ArrayList<>();
    private final List<OptionBuilder> looseOptions = new ArrayList<>();

    @Override
    void prepareBuild() {
        super.prepareBuild();

        Validate.notEmpty(this.groups, "At least one group must be added");
    }

    @Override
    OptionPage build() {
        this.prepareBuild();

        if (!this.looseOptions.isEmpty()) {
            var implicitGroup = new OptionGroupBuilderImpl();
            this.looseOptions.forEach(implicitGroup::addOption);
            this.addOptionGroup(implicitGroup);
        }

        return new OptionPage(this.name, ImmutableList.copyOf(this.groups));
    }

    @Override
    public OptionPageBuilder addOptionGroup(OptionGroupBuilder group) {
        this.groups.add(((OptionGroupBuilderImpl) group).build());
        return this;
    }

    @Override
    public OptionPageBuilder addOption(OptionBuilder option) {
        this.looseOptions.add(option);
        return this;
    }

    @Override
    public OptionPageBuilderImpl setName(Component name) {
        super.setName(name);
        return this;
    }
}
