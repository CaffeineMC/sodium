package net.caffeinemc.mods.sodium.mixin.platform.neoforge;

import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBlockRenderContext.BlockEmitter.class)
public abstract class AbstractBlockRenderContextMixin {
    @Unique
    private AbstractBlockRenderContext parent;

    /**
     * @author IMS
     * @reason Access parent
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(AbstractBlockRenderContext parent, CallbackInfo ci) {
        this.parent = parent;
    }
}
