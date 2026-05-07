package net.caffeinemc.mods.sodium.mixin.features.gui;

import com.mojang.serialization.Codec;
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

import static net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder.*;

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
                new OptionInstance.IntRange(FRAMERATE_LIMIT_MIN, FRAMERATE_LIMIT_MAX),
                Codec.intRange(FRAMERATE_LIMIT_MIN, FRAMERATE_LIMIT_MAX),
                FRAMERATE_LIMIT_DEFAULT,
                FramerateLimitOptionMixin::setFramerateLimit);
    }

    private static Component formatFramerateLimit(Component caption, Integer value) {
        if (value == FRAMERATE_LIMIT_MAX) {
            return Options.genericValueLabel(caption, Component.translatable("options.framerateLimit.max"));
        }

        return Options.genericValueLabel(caption, Component.translatable("options.framerate", value));
    }

    private static void setFramerateLimit(Integer value) {
        Minecraft.getInstance().getFramerateLimitTracker().setFramerateLimit(value);
    }
}
