package net.caffeinemc.mods.sodium.mixin.features.render.model.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.client.model.quad.BakedQuadView;
import net.caffeinemc.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils;
import net.caffeinemc.mods.sodium.client.util.DirectionUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.neoforged.neoforge.client.RenderTypeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererMixin {
    @Unique
    private static final ThreadLocal<RandomSource> RANDOM = ThreadLocal.withInitial(() -> new SingleThreadedRandomSource(42L));

    @Unique
    private static final ThreadLocal<List<BlockModelPart>> LIST = ThreadLocal.withInitial(() -> new ObjectArrayList<>());

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void renderQuads(PoseStack.Pose matrices, VertexBufferWriter writer, int defaultColor, List<BakedQuad> quads, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);

            if (bakedQuad.vertices().length < 32) {
                continue; // ignore bad quads
            }

            BakedQuadView quad = (BakedQuadView) (Object) bakedQuad;

            int color = quad.hasColor() ? defaultColor : 0xFFFFFFFF;

            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay, false);

            if (quad.getSprite() != null) {
                SpriteUtil.INSTANCE.markSpriteActive(quad.getSprite());
            }
        }
    }

    /**
     * @reason Use optimized vertex writer intrinsics, avoid allocations
     * @author JellySquid
     */
    @Inject(method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/block/model/BlockStateModel;FFFIILnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("HEAD"), cancellable = true)
    private static void renderFast(PoseStack.Pose entry, MultiBufferSource bufferSource, BlockStateModel bakedModel, float red, float green, float blue, int light, int overlay, BlockAndTintGetter level, BlockPos pos, BlockState state, CallbackInfo ci) {
        RandomSource random = RANDOM.get();

        // Clamp color ranges
        red = Mth.clamp(red, 0.0F, 1.0F);
        green = Mth.clamp(green, 0.0F, 1.0F);
        blue = Mth.clamp(blue, 0.0F, 1.0F);

        int defaultColor = ColorABGR.pack(red, green, blue, 1.0F);
        random.setSeed(42L);

        List<BlockModelPart> list = LIST.get();

        list.clear();

        bakedModel.collectParts(level, pos, state, random, list);

        for (BlockModelPart part : list) {
            var writer = VertexBufferWriter.of(bufferSource.getBuffer(RenderTypeHelper.getEntityRenderType(part.getRenderType(state))));

            if (writer == null) {
                return;
            }

            for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
                List<BakedQuad> quads = part.getQuads(direction);

                if (!quads.isEmpty()) {
                    renderQuads(entry, writer, defaultColor, quads, light, overlay);
                }
            }

            List<BakedQuad> quads = part.getQuads(null);

            if (!quads.isEmpty()) {
                renderQuads(entry, writer, defaultColor, quads, light, overlay);
            }
        }

        ci.cancel();
    }
}
