package net.caffeinemc.mods.sodium.mixin.features.render.immediate.buffer_builder.intrinsics;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.model.quad.ModelQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@SuppressWarnings({ "SameParameterValue" })
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumer {
    @Shadow
    @Final
    private boolean fastFormat;

    @Shadow
    @Final
    private boolean fullFormat;

    @Override
    public void putBakedQuad(PoseStack.Pose pose, BakedQuad quad, QuadInstance instance) {
        if (!this.fastFormat) { // check for ENTITY.
            VertexConsumer.super.putBakedQuad(pose, quad, instance);

            if (quad.materialInfo().sprite() != null) {
                SpriteUtil.INSTANCE.markSpriteActive(quad.materialInfo().sprite());
            }

            return;
        }

        VertexBufferWriter writer = VertexBufferWriter.of(this);

        ModelQuadView quadX = (ModelQuadView) (Object) quad;

        BakedModelEncoder.writeQuadVertices(writer, pose, quadX, instance);

        if (quad.materialInfo().sprite() != null) {
            SpriteUtil.INSTANCE.markSpriteActive(quad.materialInfo().sprite());
        }
    }
}
