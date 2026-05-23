package net.caffeinemc.mods.sodium.mixin.core.render.world;

import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ViewArea.class)
public class ViewAreaMixin {
    @Inject(method = "lambda$new$0", at = @At("HEAD"), cancellable = true)
    private static void sodium$safelyReturn(SectionRenderDispatcher sectionRenderDispatcher, int index, long sectionNode, CallbackInfoReturnable<SectionRenderDispatcher.RenderSection> cir) {
        if (sectionRenderDispatcher == null) cir.setReturnValue(null);
    }
}
