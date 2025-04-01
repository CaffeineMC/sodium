package net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_upload;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import com.mojang.blaze3d.opengl.GlBuffer;

@Mixin(GlBuffer.class)
public interface GlBufferAccessor {

    @Accessor
    void setInitialized(boolean value);
}
