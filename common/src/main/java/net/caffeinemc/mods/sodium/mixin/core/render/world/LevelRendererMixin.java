package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.FilterMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.util.GameRendererStorage;
import net.caffeinemc.mods.sodium.client.util.IgnoringSectionRenderDispatcher;
import net.caffeinemc.mods.sodium.client.util.IgnoringViewArea;
import net.caffeinemc.mods.sodium.client.util.SodiumChunkSection;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExtension {
    @Unique
    private static final EnumMap<ChunkSectionLayer,Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> STATIC_MAP = new EnumMap<>(ChunkSectionLayer.class);

    @Shadow
    @Final
    private RenderBuffers renderBuffers;


    @Shadow
    @Final
    private WorldBorderRenderer worldBorderRenderer;

    @Shadow
    @Final
    private SubmitNodeStorage submitNodeStorage;
    @Shadow
    @Final
    private LevelRenderState levelRenderState;
    @Shadow
    @Final
    private CloudRenderer cloudRenderer;

    @Shadow
    public abstract void clearVisibleSections();

    @Shadow
    private @Nullable ViewArea viewArea;
    @Shadow
    private @Nullable SectionRenderDispatcher sectionRenderDispatcher;
    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;
    @Unique
    private SodiumWorldRenderer renderer;

    @Unique
    private ChunkRenderMatrices matrices;

    @Override
    public SodiumWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Redirect(method = "invalidateCompiledGeometry", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I", ordinal = 0))
    private int nullifyBuiltChunkStorage(Options options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, ModelManager modelManager, TextureManager textureManager, AtlasManager atlasManager, ShaderManager shaderManager, GameRenderer gameRenderer, int width, int height, CallbackInfo ci) {
        this.renderer = new SodiumWorldRenderer(Minecraft.getInstance());
    }

    /**
     * @reason Redirect the check to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean hasRenderedAllSections() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "resetLevelRenderData", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    /**
     * @reason Redirect to our renderer
     * @author IMS
     */
    @Overwrite
    public ChunkSectionsToRender prepareChunkRenders(Matrix4fc matrix4fc) {
        return new ChunkSectionsToRender(Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView(), STATIC_MAP, -1, new GpuBufferSlice[0]);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V"))
    private void getRenderState(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, CallbackInfo ci, @Local ChunkSectionsToRender chunkSectionsToRender) {
        matrices = new ChunkRenderMatrices(((GameRendererStorage) Minecraft.getInstance().gameRenderer).sodium$getProjectionMatrix(), modelViewMatrix);
        ((SodiumChunkSection) (Object) chunkSectionsToRender).sodium$setRendering(renderer, matrices, this.levelRenderState.cameraRenderState.pos.x, this.levelRenderState.cameraRenderState.pos.y, this.levelRenderState.cameraRenderState.pos.z);

        // update the fog color here with the actual fog color being used to render the sky, since the fog color that SodiumWorldRenderer still has stored from FogRendererMixin is outdated.
        this.renderer.updateFogColor(fogColor);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean isSectionCompiledAndVisible(BlockPos pos) {
        return this.renderer.isSectionReady(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), cancellable = true)
    private void sodium$replace(ClientLevel level, Options options, Camera camera, BlockColors blockColors, CallbackInfo ci) {
        ci.cancel();

        this.cloudRenderer.markForRebuild();
        LeavesBlock.setCutoutLeaves(options.cutoutLeaves().get());

        this.renderer.reload();

        this.sectionRenderDispatcher = new IgnoringSectionRenderDispatcher(Util.backgroundExecutor(), this.renderBuffers, null, this.sectionOcclusionGraph::schedulePropagationFrom);
        this.viewArea = new IgnoringViewArea(sectionRenderDispatcher);
        this.sectionOcclusionGraph .waitAndReset(this.viewArea);

        this.clearVisibleSections();
    }

    // Exclusive to NeoForge, allow to fail.
    @SuppressWarnings("all")
    @Inject(method = "iterateVisibleBlockEntities", at = @At("HEAD"), cancellable = true, require = 0)
    public void replaceBlockEntityIteration(Consumer<BlockEntity> blockEntityConsumer, CallbackInfo ci) {
        ci.cancel();

        this.renderer.iterateVisibleBlockEntities(blockEntityConsumer);
    }

    /**
     * @reason Allow control of the texture filtering mode
     * @author pajic
     */
    @Redirect(method = "lambda$addMainPass$0", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/textures/FilterMode;LINEAR:Lcom/mojang/blaze3d/textures/FilterMode;", opcode = Opcodes.GETSTATIC))
    private FilterMode setFilterMode() {
        return SodiumClientMod.options().quality.pixelFilteringMode;
    }

    @Override
    public void sodium$setMatrices(ChunkRenderMatrices matrices) {
        this.matrices = matrices;
    }

    @Override
    public ChunkRenderMatrices sodium$getMatrices() {
        return this.matrices;
    }
}
