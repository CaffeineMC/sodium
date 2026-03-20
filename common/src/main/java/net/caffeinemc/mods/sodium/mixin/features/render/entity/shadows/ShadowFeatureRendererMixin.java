package net.caffeinemc.mods.sodium.mixin.features.render.entity.shadows;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowFeatureRenderer.class)
public class ShadowFeatureRendererMixin {
    @Unique
    private static final int DEFAULT_NORMAL = NormI8.pack(0.0f, 1.0f, 0.0f);

    @Shadow
    @Final
    private static RenderType SHADOW_RENDER_TYPE;

    @Unique
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);

    /**
     * @author JellySquid
     * @reason Reduce vertex assembly overhead for shadow rendering
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void renderShadowPartFast(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource, CallbackInfo ci) {
        VertexConsumer vertices = bufferSource.getBuffer(SHADOW_RENDER_TYPE);

        var writer = VertexConsumerUtils.convertOrLog(vertices);

        if (writer == null) {
            return;
        }

        ci.cancel();

        for (SubmitNodeStorage.ShadowSubmit shadows : submitNodeCollection.getShadowSubmits()) {
            Matrix4f matrices = shadows.pose();

            for (int i = 0; i < shadows.pieces().size(); i++) {
                EntityRenderState.ShadowPiece shadowPiece = shadows.pieces().get(i);

                float alpha = shadowPiece.alpha();

                if (alpha >= 0.0F) {
                    if (alpha > 1.0F) {
                        alpha = 1.0F;
                    }

                    AABB box = shadowPiece.shapeBelow().bounds();

                    float minX = (float) (shadowPiece.relativeX() + box.minX);
                    float maxX = (float) (shadowPiece.relativeX() + box.maxX);
                    float minY = (float) (shadowPiece.relativeY() + box.minY);
                    float minZ = (float) (shadowPiece.relativeZ() + box.minZ);
                    float maxZ = (float) (shadowPiece.relativeZ() + box.maxZ);

                    renderShadowPart(matrices, writer, shadows.radius(), alpha, minX, maxX, minY, minZ, maxZ);
                }
            }
        }
    }

    @Unique
    private static void renderShadowPart(Matrix4f matPosition, VertexBufferWriter writer, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        float size = 0.5F * (1.0F / radius);

        float u1 = (-minX * size) + 0.5F;
        float u2 = (-maxX * size) + 0.5F;

        float v1 = (-minZ * size) + 0.5F;
        float v2 = (-maxZ * size) + 0.5F;


        var color = ColorABGR.withAlpha(SHADOW_COLOR, alpha);
        var normal = DEFAULT_NORMAL; // This seems wrong, but it is identical to Vanilla's handling.

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * EntityVertex.STRIDE);
            long ptr = buffer;

            writeShadowVertex(ptr, matPosition, minX, minY, minZ, u1, v1, color, normal);
            ptr += EntityVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, minX, minY, maxZ, u1, v2, color, normal);
            ptr += EntityVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, maxZ, u2, v2, color, normal);
            ptr += EntityVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, minZ, u2, v1, color, normal);
            ptr += EntityVertex.STRIDE;

            writer.push(stack, buffer, 4, EntityVertex.FORMAT);
        }
    }

    @Unique
    private static void writeShadowVertex(long ptr, Matrix4f matPosition, float x, float y, float z, float u, float v, int color, int normal) {
        // The transformed position vector
        float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
        float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
        float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

        EntityVertex.write(ptr, xt, yt, zt, color, u, v, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, normal);
    }
}
