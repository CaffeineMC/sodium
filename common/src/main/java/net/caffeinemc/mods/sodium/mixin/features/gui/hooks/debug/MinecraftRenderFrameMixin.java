package net.caffeinemc.mods.sodium.mixin.features.gui.hooks.debug;

import net.caffeinemc.mods.sodium.client.util.FrameTimeStatistics;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftRenderFrameMixin {
    // hook the vanilla fps update to update our fps display only when it does too, once a second
    @Inject(
            method = "runTick",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;fps:I", opcode = Opcodes.PUTSTATIC, shift = At.Shift.AFTER)
    )
    private void sodium$updatePercentileCache(boolean advanceGameTime, CallbackInfo ci) {
        FrameTimeStatistics.INSTANCE.invalidate();
    }
}
