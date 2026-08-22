package net.caffeinemc.mods.sodium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.mojang.blaze3d.opengl.GlCommandEncoder")
public class GlFenceSyncMixin {

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glFenceSync(II)J"))
    private long sodium$disableGlFenceSync(int condition, int flags) {
        return 0L;
    }

}
