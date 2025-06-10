package net.caffeinemc.mods.sodium.client.render.frapi.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipeline;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import net.caffeinemc.mods.sodium.client.render.frapi.SodiumRenderer;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.EncodingFormat;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.services.PlatformBlockAccess;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Base class for the functions that can be shared between the terrain and non-terrain pipelines.
 *
 * <p>Make sure to set the {@link #lighters} in the subclass constructor.
 */
public abstract class AbstractBlockRenderContext extends AbstractRenderContext {
    public class BlockEmitter extends MutableQuadViewImpl {
        private final List<BlockModelPart> cachedList = new ObjectArrayList<>();

        {
            data = new int[EncodingFormat.TOTAL_STRIDE];
            clear();
        }

        @Override
        public void emitDirectly() {
            renderQuad(this);
        }

        public void markInvalidToDowngrade() {
            AbstractBlockRenderContext.this.allowDowngrade = false;
        }

        public void emitPart(BlockModelPart part, Predicate<@Nullable Direction> cullTest) {
            AbstractBlockRenderContext.this.bufferDefaultModel(part, cullTest);
        }

        public List<BlockModelPart> cachedList() {
            return cachedList;
        }
    }



    private final BlockEmitter editorQuad = new BlockEmitter();

    /**
     * The world which the block is being rendered in.
     */
    protected BlockAndTintGetter level;
    /**
     * The level slice used for rendering
     */
    protected LevelSlice slice;
    /**
     * The state of the block being rendered.
     */
    protected BlockState state;
    /**
     * The position (in world space) of the block being rendered.
     */
    protected BlockPos pos;

    protected ChunkSectionLayer defaultRenderType;

    protected boolean allowDowngrade;

    private final BlockOcclusionCache occlusionCache = new BlockOcclusionCache();
    private boolean enableCulling = true;
    // Cull cache (as it's checked per-quad instead of once per side like in vanilla)
    private int cullCompletionFlags;
    private int cullResultFlags;

    protected RandomSource random;

    /**
     * Must be set by the subclass constructor.
     */
    protected LightPipelineProvider lighters;
    protected final QuadLightData quadLightData = new QuadLightData();
    protected boolean useAmbientOcclusion;
    // Default AO mode for model (can be overridden by material property)
    protected LightMode defaultLightMode;

    @Override
    public QuadEmitter getEmitter() {
        this.editorQuad.clear();
        return this.editorQuad;
    }

    public boolean isFaceCulled(@Nullable Direction face) {
        if (face == null || !this.enableCulling) {
            return false;
        }

        final int mask = 1 << face.get3DDataValue();

        if ((this.cullCompletionFlags & mask) == 0) {
            this.cullCompletionFlags |= mask;

            if (this.occlusionCache.shouldDrawSide(this.state, this.level, this.pos, face)) {
                this.cullResultFlags |= mask;
                return false;
            } else {
                return true;
            }
        } else {
            return (this.cullResultFlags & mask) == 0;
        }
    }

    /**
     * Pipeline entrypoint - handles transform and culling checks.
     */
    private void renderQuad(MutableQuadViewImpl quad) {
        if (this.isFaceCulled(quad.cullFace())) {
            return;
        }

        this.processQuad(quad);
    }

    /**
     * Quad pipeline function - after transform and culling checks.
     * Can also be used as entrypoint to skip some logic if the transform and culling checks have already been performed.
     */
    protected abstract void processQuad(MutableQuadViewImpl quad);

    protected void prepareCulling(boolean enableCulling) {
        this.enableCulling = enableCulling;
        this.cullCompletionFlags = 0;
        this.cullResultFlags = 0;
    }

    protected void prepareAoInfo(boolean modelAo) {
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
        // Ignore the incorrect IDEA warning here.
        this.defaultLightMode = this.useAmbientOcclusion && modelAo && (state != null && PlatformBlockAccess.getInstance().getLightEmission(state, level, pos) == 0) ? LightMode.SMOOTH : LightMode.FLAT;
    }

    protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, ShadeMode shadeMode) {
        LightPipeline lighter = this.lighters.getLighter(lightMode);
        QuadLightData data = this.quadLightData;
        lighter.calculate(quad, this.pos, data, quad.cullFace(), quad.lightFace(), quad.hasShade(), shadeMode == ShadeMode.ENHANCED);

        if (emissive) {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightTexture.FULL_BRIGHT);
            }
        } else {
            int[] lightmaps = data.lm;

            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmaps[i]));
            }
        }
    }

    private List<BlockModelPart> parts = new ObjectArrayList<>();

    /* Handling of vanilla models - this is the hot path for non-modded models */
    public void bufferDefaultModel(BlockModelPart part, Predicate<Direction> cullTest) {
        MutableQuadViewImpl editorQuad = this.editorQuad;
        this.prepareAoInfo(part.useAmbientOcclusion());

        ChunkSectionLayer renderType = PlatformModelAccess.getInstance().getPartRenderType(part, state, this.defaultRenderType);
        ChunkSectionLayer defaultType = this.defaultRenderType;
        this.defaultRenderType = renderType;

        for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
            final Direction cullFace = ModelHelper.faceFromIndex(i);

            if (cullTest.test(cullFace)) {
                continue;
            }

            // TODO NeoForge 1.21.5
            AmbientOcclusionMode ao = PlatformBlockAccess.getInstance().usesAmbientOcclusion(part, state, renderType, slice, pos);

            final List<BakedQuad> quads = PlatformModelAccess.getInstance().getQuads(level, pos, part, state, cullFace, random, renderType);
            final int count = quads.size();

            for (int j = 0; j < count; j++) {
                final BakedQuad q = quads.get(j);
                editorQuad.fromBakedQuad(q);
                editorQuad.cullFace(cullFace);
                editorQuad.renderLayer(renderType);
                editorQuad.ambientOcclusion(ao.toTriState());
                // Call processQuad instead of emit for efficiency
                // (avoid unnecessarily clearing data, trying to apply transforms, and performing cull check again)

                editorQuad.transformAndEmit();
            }
        }

        editorQuad.clear();

        this.defaultRenderType = defaultType;
    }
}
