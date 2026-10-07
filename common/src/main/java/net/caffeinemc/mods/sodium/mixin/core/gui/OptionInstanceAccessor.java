package net.caffeinemc.mods.sodium.mixin.core.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor<T> {
    @Accessor("initialValue")
    T sodium$getInitialValue();

    @Accessor("caption")
    Component sodium$getCaption();

    @Accessor("tooltip")
    OptionInstance.TooltipSupplier<T> sodium$getTooltip();

    @Accessor("toString")
    Function<T, Component> sodium$getToString();
}
