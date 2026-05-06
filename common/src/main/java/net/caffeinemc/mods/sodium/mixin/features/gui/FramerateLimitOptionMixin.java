package net.caffeinemc.mods.sodium.mixin.features.gui;

import com.mojang.serialization.Codec;
import net.caffeinemc.mods.sodium.client.gui.options.FramerateLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class FramerateLimitOptionMixin {
    @Mutable
    @Shadow
    @Final
    private OptionInstance<Integer> framerateLimit;

    @Inject(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Options;framerateLimit:Lnet/minecraft/client/OptionInstance;",
                    opcode = Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            )
    )
    private void replaceFramerateLimitOption(Minecraft minecraft, java.io.File optionsFile, CallbackInfo ci) {
        this.framerateLimit = new OptionInstance<>(
                "options.framerateLimit",
                OptionInstance.noTooltip(),
                FramerateLimitOptionMixin::formatFramerateLimit,
                new OptionInstance.IntRange(FramerateLimit.MIN, FramerateLimit.MAX),
                Codec.intRange(FramerateLimit.MIN, FramerateLimit.MAX),
                FramerateLimit.VANILLA_DEFAULT,
                FramerateLimitOptionMixin::setFramerateLimit);
    }

    private static Component formatFramerateLimit(Component caption, Integer value) {
        if (value == FramerateLimit.MAX) {
            return Options.genericValueLabel(caption, Component.translatable("options.framerateLimit.max"));
        }

        return Options.genericValueLabel(caption, Component.translatable("options.framerate", value));
    }

    private static void setFramerateLimit(Integer value) {
        Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(value);
    }
}
