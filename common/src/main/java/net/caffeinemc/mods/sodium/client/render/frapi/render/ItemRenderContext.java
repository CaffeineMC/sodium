/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.caffeinemc.mods.sodium.client.render.frapi.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.MatrixUtil;
import net.caffeinemc.mods.sodium.api.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.caffeinemc.mods.sodium.mixin.features.render.frapi.ItemRendererAccessor;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.function.Supplier;

/**
 * The render context used for item rendering.
 */
public class ItemRenderContext extends AbstractRenderContext {
    /** Value vanilla uses for item rendering.  The only sensible choice, of course.  */
    private static final long ITEM_RANDOM_SEED = 42L;

    private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
        {
            this.data = new int[EncodingFormat.TOTAL_STRIDE];
            this.clear();
        }

        @Override
        public void emitDirectly() {
            ItemRenderContext.this.renderQuad(this);
        }
    };

    @Deprecated
    private final BakedModelConsumerImpl vanillaModelConsumer = new BakedModelConsumerImpl();

    private final ItemColors colorMap;
    private final VanillaModelBufferer vanillaBufferer;

    private final RandomSource random = new SingleThreadedRandomSource(ITEM_RANDOM_SEED);
    private final Supplier<RandomSource> randomSupplier = () -> {
        this.random.setSeed(ITEM_RANDOM_SEED);
        return this.random;
    };

    private ItemStack itemStack;
    private ItemDisplayContext transformMode;
    private PoseStack poseStack;
    private Matrix4f matPosition;
    private boolean trustedNormals;
    private Matrix3f matNormal;
    private MultiBufferSource bufferSource;
    private int lightmap;
    private int overlay;

    private boolean isDefaultTranslucent;
    private boolean isTranslucentDirect;
    private boolean isDefaultGlint;
    private boolean isGlintDynamicDisplay;

    private PoseStack.Pose dynamicDisplayGlintEntry;
    private VertexConsumer translucentVertexConsumer;
    private VertexConsumer cutoutVertexConsumer;
    private VertexConsumer translucentGlintVertexConsumer;
    private VertexConsumer cutoutGlintVertexConsumer;
    private VertexConsumer defaultVertexConsumer;

    public ItemRenderContext(ItemColors colorMap, VanillaModelBufferer vanillaBufferer) {
        this.colorMap = colorMap;
        this.vanillaBufferer = vanillaBufferer;
    }

    @Override
    public QuadEmitter getEmitter() {
        this.editorQuad.clear();
        return this.editorQuad;
    }

    @Override
    public boolean isFaceCulled(@Nullable Direction face) {
        throw new UnsupportedOperationException("isFaceCulled can only be called on a block render context.");
    }

    @Override
    public ItemDisplayContext itemTransformationMode() {
        return this.transformMode;
    }

    @Deprecated
    @Override
    public BakedModelConsumer bakedModelConsumer() {
        return this.vanillaModelConsumer;
    }

    public void renderModel(ItemStack itemStack, ItemDisplayContext transformMode, boolean invert, PoseStack poseStack, MultiBufferSource bufferSource, int lightmap, int overlay, BakedModel model) {
        this.itemStack = itemStack;
        this.transformMode = transformMode;
        this.poseStack = poseStack;
        this.matPosition = poseStack.last().pose();
        this.trustedNormals = poseStack.last().trustedNormals;
        this.matNormal = poseStack.last().normal();
        this.bufferSource = bufferSource;
        this.lightmap = lightmap;
        this.overlay = overlay;
        this.computeOutputInfo();

        ((FabricBakedModel) model).emitItemQuads(itemStack, this.randomSupplier, this);

        this.itemStack = null;
        this.poseStack = null;
        this.bufferSource = null;

        this.dynamicDisplayGlintEntry = null;
        this.translucentVertexConsumer = null;
        this.cutoutVertexConsumer = null;
        this.translucentGlintVertexConsumer = null;
        this.cutoutGlintVertexConsumer = null;
        this.defaultVertexConsumer = null;
    }

    private void computeOutputInfo() {
        this.isDefaultTranslucent = true;
        this.isTranslucentDirect = true;

        Item item = this.itemStack.getItem();

        if (item instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);

            if (renderType != RenderType.translucent()) {
                this.isDefaultTranslucent = false;
            }

            if (this.transformMode != ItemDisplayContext.GUI && !this.transformMode.firstPerson()) {
                this.isTranslucentDirect = false;
            }
        }

        this.isDefaultGlint = this.itemStack.hasFoil();
        this.isGlintDynamicDisplay = ItemRendererAccessor.sodium$hasAnimatedTexture(this.itemStack);

        this.defaultVertexConsumer = this.getVertexConsumer(BlendMode.DEFAULT, TriState.DEFAULT);
    }

    private void renderQuad(MutableQuadViewImpl quad) {
        if (!this.transform(quad)) {
            return;
        }

        final RenderMaterial mat = quad.material();
        final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
        final boolean emissive = mat.emissive();
        final VertexConsumer vertexConsumer = this.getVertexConsumer(mat.blendMode(), mat.glint());

        this.colorizeQuad(quad, colorIndex);
        this.shadeQuad(quad, emissive);
        this.bufferQuad(quad, vertexConsumer);
    }

    private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex != -1) {
            final int itemColor = this.colorMap.getColor(this.itemStack, colorIndex);

            for (int i = 0; i < 4; i++) {
                quad.color(i, ColorMixer.mulComponentWise(itemColor, quad.color(i)));
            }
        }
    }

    private void shadeQuad(MutableQuadViewImpl quad, boolean emissive) {
        if (emissive) {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightTexture.FULL_BRIGHT);
            }
        } else {
            final int lightmap = this.lightmap;

            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmap));
            }
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
        QuadEncoder.writeQuadVertices(quad, vertexConsumer, this.overlay, this.matPosition, this.trustedNormals, this.matNormal);
        var sprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
        if (sprite != null) {
            SpriteUtil.INSTANCE.markSpriteActive(sprite);
        }
    }

    /**
     * Caches custom blend mode / vertex consumers and mimics the logic
     * in {@code RenderLayers.getEntityBlockLayer}. Layers other than
     * translucent are mapped to cutout.
     */
    private VertexConsumer getVertexConsumer(BlendMode blendMode, TriState glintMode) {
        boolean translucent;
        boolean glint;

        if (blendMode == BlendMode.DEFAULT) {
            translucent = this.isDefaultTranslucent;
        } else {
            translucent = blendMode == BlendMode.TRANSLUCENT;
        }

        if (glintMode == TriState.DEFAULT) {
            glint = this.isDefaultGlint;
        } else {
            glint = glintMode == TriState.TRUE;
        }

        if (translucent) {
            if (glint) {
                if (this.translucentGlintVertexConsumer == null) {
                    this.translucentGlintVertexConsumer = this.createTranslucentVertexConsumer(true);
                }

                return this.translucentGlintVertexConsumer;
            } else {
                if (this.translucentVertexConsumer == null) {
                    this.translucentVertexConsumer = this.createTranslucentVertexConsumer(false);
                }

                return this.translucentVertexConsumer;
            }
        } else {
            if (glint) {
                if (this.cutoutGlintVertexConsumer == null) {
                    this.cutoutGlintVertexConsumer = this.createCutoutVertexConsumer(true);
                }

                return this.cutoutGlintVertexConsumer;
            } else {
                if (this.cutoutVertexConsumer == null) {
                    this.cutoutVertexConsumer = this.createCutoutVertexConsumer(false);
                }

                return this.cutoutVertexConsumer;
            }
        }
    }

    private VertexConsumer createTranslucentVertexConsumer(boolean glint) {
        if (glint && this.isGlintDynamicDisplay) {
            return this.createDynamicDisplayGlintVertexConsumer(Minecraft.useShaderTransparency() && !this.isTranslucentDirect ? Sheets.translucentItemSheet() : Sheets.translucentCullBlockSheet());
        }

        if (this.isTranslucentDirect) {
            return ItemRenderer.getFoilBufferDirect(this.bufferSource, Sheets.translucentCullBlockSheet(), true, glint);
        } else if (Minecraft.useShaderTransparency()) {
            return ItemRenderer.getFoilBuffer(this.bufferSource, Sheets.translucentItemSheet(), true, glint);
        } else {
            return ItemRenderer.getFoilBuffer(this.bufferSource, Sheets.translucentItemSheet(), true, glint);
        }
    }

    private VertexConsumer createCutoutVertexConsumer(boolean glint) {
        if (glint && this.isGlintDynamicDisplay) {
            return this.createDynamicDisplayGlintVertexConsumer(Sheets.cutoutBlockSheet());
        }

        return ItemRenderer.getFoilBufferDirect(this.bufferSource, Sheets.cutoutBlockSheet(), true, glint);
    }

    private VertexConsumer createDynamicDisplayGlintVertexConsumer(RenderType type) {
        if (this.dynamicDisplayGlintEntry == null) {
            this.dynamicDisplayGlintEntry = this.poseStack.last().copy();

            if (this.transformMode == ItemDisplayContext.GUI) {
                MatrixUtil.mulComponentWise(this.dynamicDisplayGlintEntry.pose(), 0.5F);
            } else if (this.transformMode.firstPerson()) {
                MatrixUtil.mulComponentWise(this.dynamicDisplayGlintEntry.pose(), 0.75F);
            }
        }

        return ItemRenderer.getCompassFoilBuffer(this.bufferSource, type, this.dynamicDisplayGlintEntry);
    }

    public void bufferDefaultModel(BakedModel model, @Nullable BlockState state) {
        if (this.hasTransform() || this.vanillaBufferer == null) {
            VanillaModelEncoder.emitItemQuads(model, state, this.randomSupplier, this);
        } else {
            this.vanillaBufferer.accept(model, this.itemStack, this.lightmap, this.overlay, this.poseStack, this.defaultVertexConsumer);
        }
    }

    @Deprecated
    private class BakedModelConsumerImpl implements BakedModelConsumer {
        @Override
        public void accept(BakedModel model) {
            this.accept(model, null);
        }

        @Override
        public void accept(BakedModel model, @Nullable BlockState state) {
            ItemRenderContext.this.bufferDefaultModel(model, state);
        }
    }

    /** used to accept a method reference from the ItemRenderer. */
    @FunctionalInterface
    public interface VanillaModelBufferer {
        void accept(BakedModel model, ItemStack stack, int color, int overlay, PoseStack matrixStack, VertexConsumer buffer);
    }
}
