package net.caffeinemc.mods.sodium.mixin.debug;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

// for flying faster in debugging
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @ModifyConstant(method = "onScroll", constant = @Constant(floatValue = 0.2f))
    private float sodium$raiseSpectatorFlySpeedCap(float original) {
        return 5.0f;
    }

    @ModifyConstant(method = "onScroll", constant = @Constant(floatValue = 0.005f))
    private float sodium$raiseSpectatorFlyStep(float original) {
        return 0.1f;
    }
}
