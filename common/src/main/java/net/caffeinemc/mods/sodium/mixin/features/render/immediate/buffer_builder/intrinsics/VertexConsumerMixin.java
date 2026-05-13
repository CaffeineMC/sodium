package net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_builder.intrinsics;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
    @WrapMethod(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFII)V")
    default void sodium$modifyPutBulkData(PoseStack.Pose pose, BakedQuad bakedQuad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, Operation<Void> original) {
        if ((Object) this instanceof BufferBuilder bufferBuilder) {
            if (!((BufferBuilderAccessor) bufferBuilder).sodium$fastFormat()) {
                original.call(pose, bakedQuad, red, green, blue, alpha, packedLight, packedOverlay);

                if (bakedQuad.getSprite() != null) {
                    SpriteUtil.INSTANCE.markSpriteActive(bakedQuad.getSprite());
                }

                return;
            }

            if (bakedQuad.getVertices().length < 32) {
                return; // we do not accept quads with less than 4 properly sized vertices
            }

            VertexBufferWriter writer = VertexBufferWriter.of((VertexConsumer) this);

            ModelQuadView quad = (ModelQuadView) bakedQuad;

            int color = ColorABGR.pack(red, green, blue, alpha);
            BakedModelEncoder.writeQuadVertices(writer, pose, quad, color, packedLight, packedOverlay, false);

            if (quad.getSprite() != null) {
                SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
            }
        } else {
            original.call(pose, bakedQuad, red, green, blue, alpha, packedLight, packedOverlay);
        }
    }
    
    @WrapMethod(method = "putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;[FFFFF[IIZ)V")
    default void sodium$modifyPutBulkData(PoseStack.Pose pose, BakedQuad bakedQuad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readAlpha, Operation<Void> original) {
        if ((Object) this instanceof BufferBuilder bufferBuilder) {
            if (!((BufferBuilderAccessor) bufferBuilder).sodium$fastFormat()) {
                original.call(pose, bakedQuad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readAlpha);

                if (bakedQuad.getSprite() != null) {
                    SpriteUtil.INSTANCE.markSpriteActive(bakedQuad.getSprite());
                }

                return;
            }

            if (bakedQuad.getVertices().length < 32) {
                return; // we do not accept quads with less than 4 properly sized vertices
            }

            VertexBufferWriter writer = VertexBufferWriter.of((VertexConsumer) this);

            ModelQuadView quad = (ModelQuadView) bakedQuad;

            BakedModelEncoder.writeQuadVertices(writer, pose, quad, red, green, blue, alpha, brightness, readAlpha, lightmap, packedOverlay);

            if (quad.getSprite() != null) {
                SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
            }
        } else {
            original.call(pose, bakedQuad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readAlpha);
        }
    }
}
