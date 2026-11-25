package net.caffeinemc.mods.sodium.mixin.core.gui;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Tooltip.class)
public interface TooltipAccessor {
    @Accessor("messages")
    List<Component> sodium$getMessage();
}
