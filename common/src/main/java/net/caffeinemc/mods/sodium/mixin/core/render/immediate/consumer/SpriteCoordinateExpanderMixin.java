package net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatOffsetCache;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.UvMapping;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteCoordinateExpander.class)
public class SpriteCoordinateExpanderMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private UvMapping mapping;
    @Unique
    private boolean canUseIntrinsics;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(VertexConsumer delegate, UvMapping mapping, CallbackInfo ci) {
        this.canUseIntrinsics = VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.canUseIntrinsics;
    }

    @Override
    public void push(MemoryStack stack, final long ptr, int count, VertexFormat format) {
        transform(ptr, count, format, this.mapping);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the texture UVs for each vertex from their absolute coordinates into the sprite area specified
     * by the parameters.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param mapping The UV mapping
     */
    @Unique
    private static void transform(long ptr, int count, VertexFormat format,
                                  UvMapping mapping) {
        long stride = format.getVertexSize();

        var cache = VertexFormatOffsetCache.getInstance().getCachedOffsets(format);

        var offsetUV = cache[VertexFormatOffsetCache.UV];

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            // The texture coordinates relative to the sprite bounds
            float u = TextureAttribute.getU(ptr + offsetUV);
            float v = TextureAttribute.getV(ptr + offsetUV);

            // The texture coordinates in absolute space on the sprite sheet
            float ut = mapping.getU(u);
            float vt = mapping.getV(v);

            TextureAttribute.put(ptr + offsetUV, ut, vt);

            ptr += stride;
        }
    }
}
