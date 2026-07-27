package net.caffeinemc.mods.sodium.mixin.features.render.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.format.common.GlyphVertex;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedSheetGlyph.class)
public class BakedGlyphMixin {
    @Shadow
    @Final
    private float left;

    @Shadow
    @Final
    private float right;

    @Shadow
    @Final
    private float up;

    @Shadow
    @Final
    private float down;

    @Shadow
    @Final
    private float u0;

    @Shadow
    @Final
    private float v0;

    @Shadow
    @Final
    private float v1;

    @Shadow
    @Final
    private float u1;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void drawFast(boolean italic,
                          float x,
                          float y,
                          float z,
                          Matrix4fc pose,
                          VertexConsumer builder,
                          int color,
                          boolean bold,
                          int packedLightCoords,
                          CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(builder);

        if (writer == null) {
            return;
        }

        ci.cancel();

        float x1 = x + this.left;
        float x2 = x + this.right;
        float h1 = y + this.up;
        float h2 = y + this.down;
        float w1 = italic ? 1.0F - 0.25F * this.up : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * this.down : 0.0F;
        float offset = bold ? 0.1F : 0.0F;

        color = ColorARGB.toABGR(color);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * GlyphVertex.STRIDE);
            long ptr = buffer;

            write(ptr, pose, x1 + w1 - offset, h1 - offset, z, color, this.u0, this.v0, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            write(ptr, pose, x1 + w2 - offset, h2 + offset, z, color, this.u0, this.v1, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            write(ptr, pose, x2 + w2 + offset, h2 + offset, z, color, this.u1, this.v1, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            write(ptr, pose, x2 + w1 + offset, h1 - offset, z, color, this.u1, this.v0, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            writer.push(stack, buffer, 4, GlyphVertex.FORMAT);
        }
    }

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Inject(method = "buildEffect", at = @At("HEAD"), cancellable = true)
    private void drawEffectFast(BakedSheetGlyph.EffectInstance effect,
                                float offset,
                                float z,
                                int color,
                                VertexConsumer buffer,
                                int packedLightCoords,
                                Matrix4fc pose,
                                CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(buffer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        float x1 = effect.x0();
        float x2 = effect.x1();
        float h1 = effect.y1(); // Yes, this is swapped in 1.21.6+.
        float h2 = effect.y0();

        color = ColorARGB.toABGR(color);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long mem = stack.nmalloc(4 * GlyphVertex.STRIDE);
            long ptr = mem;

            write(ptr, pose, x1 + offset, h1 + offset, z, color, this.u0, this.v0, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            write(ptr, pose, x2 + offset, h1 + offset, z, color, this.u0, this.v1, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            write(ptr, pose, x2 + offset, h2 + offset, z, color, this.u1, this.v1, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            write(ptr, pose, x1 + offset, h2 + offset, z, color, this.u1, this.v0, packedLightCoords);
            ptr += GlyphVertex.STRIDE;

            writer.push(stack, mem, 4, GlyphVertex.FORMAT);
        }
    }

    @Unique
    private static void write(long buffer,
                              Matrix4fc matrix, float x, float y, float z, int color, float u, float v, int light) {
        float x2 = MatrixHelper.transformPositionX(matrix, x, y, z);
        float y2 = MatrixHelper.transformPositionY(matrix, x, y, z);
        float z2 = MatrixHelper.transformPositionZ(matrix, x, y, z);

        GlyphVertex.put(buffer, x2, y2, z2, color, u, v, light);
    }

}
