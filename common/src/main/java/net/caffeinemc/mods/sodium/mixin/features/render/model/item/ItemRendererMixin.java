package net.caffeinemc.mods.sodium.mixin.features.render.model.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Unique
    private final RandomSource random = new SingleThreadedRandomSource(42L);

    @Shadow
    @Final
    private ItemColors itemColors;

    /**
     * @reason Avoid Allocations
     * @return JellySquid
     */
    @WrapOperation(method = "renderModelLists", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
    private RandomSource renderModelFastRandom(Operation<RandomSource> original) {
        return this.random;
    }

    /**
     * @reason Avoid Allocations
     * @return JellySquid
     */
    @WrapOperation(method = "renderModelLists", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Direction;values()[Lnet/minecraft/core/Direction;"))
    private Direction[] renderModelFastDirections(Operation<RandomSource> original) {
        return DirectionUtil.ALL_DIRECTIONS;
    }

    /**
     * @reason Avoid Allocations
     * @return JellySquid
     */
    @WrapOperation(method = "renderModelLists", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderQuadList(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Ljava/util/List;Lnet/minecraft/world/item/ItemStack;II)V"))
    private void renderModelFast(ItemRenderer itemRenderer, PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> quads, ItemStack itemStack, int light, int overlay, Operation<Void> original) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            original.call(itemRenderer, poseStack, vertexConsumer, quads, itemStack, light, overlay);
            return;
        }

        // TODO/NOTE: Should .last be a LocalRef?
        if (!quads.isEmpty()) {
            this.renderBakedItemQuads(poseStack.last(), writer, quads, itemStack, light, overlay);
        }
    }

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void renderBakedItemQuads(PoseStack.Pose matrices, VertexBufferWriter writer, List<BakedQuad> quads, ItemStack itemStack, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);

            if (bakedQuad.getVertices().length < 32) {
                continue; // ignore bad quads
            }

            BakedQuadView quad = (BakedQuadView) bakedQuad;

            int color = 0xFFFFFFFF;

            if (quad.hasColor()) {
                color = ColorARGB.toABGR((this.itemColors.getColor(itemStack, quad.getColorIndex())));
            }

            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay, BakedModelEncoder.shouldMultiplyAlpha());

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
