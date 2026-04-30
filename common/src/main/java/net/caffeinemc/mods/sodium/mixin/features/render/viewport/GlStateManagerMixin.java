package net.caffeinemc.mods.sodium.mixin.features.render.viewport;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
    @Unique
    private static int lastViewportX;
    @Unique
    private static int lastViewportY;
    @Unique
    private static int lastViewportWidth;
    @Unique
    private static int lastViewportHeight;

    @WrapWithCondition(
            method = "_viewport",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V")
    )
    private static boolean skipRedundantViewport(int x, int y, int w, int h) {
        if (x == lastViewportX && y == lastViewportY && w == lastViewportWidth && h == lastViewportHeight) {
            return false;
        }
        lastViewportX = x;
        lastViewportY = y;
        lastViewportWidth = w;
        lastViewportHeight = h;
        return true;
    }
}
