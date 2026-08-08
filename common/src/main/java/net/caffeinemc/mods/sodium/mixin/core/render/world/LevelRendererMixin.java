package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.FilterMode;
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
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.LeavesBlock;
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

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements LevelRendererExtension {
    @Unique
    private static EnumMap<ChunkSectionLayer,Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> STATIC_MAP;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

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
    @Mutable
    @Shadow
    @Final
    private boolean multiDrawIndirectAvailable;
    @Unique
    private SodiumWorldRenderer renderer;

    @Unique
    private ChunkRenderMatrices matrices;

    @Override
    public SodiumWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Redirect(
            method = "invalidateCompiledGeometry",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I",
                    ordinal = 0))
    private int nullifyBuiltChunkStorage(Options options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRenderDispatcher entityRenderDispatcher,
                      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
                      ModelManager modelManager,
                      TextureManager textureManager,
                      AtlasManager atlasManager,
                      ShaderManager shaderManager,
                      GameRenderer gameRenderer,
                      int width,
                      int height,
                      CallbackInfo ci) {
        STATIC_MAP = new EnumMap<>(ChunkSectionLayer.class);
        this.renderer = new SodiumWorldRenderer(Minecraft.getInstance());
        this.multiDrawIndirectAvailable = false;
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

    @Inject(method = "endFrame", at = @At("RETURN"))
    private void sodium$endFrame(CallbackInfo ci) {
        this.renderer.endFrame();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareChunkRenders(Lorg/joml/Matrix4fc;Z)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;"))
    private ChunkSectionsToRender getRenderState(LevelRenderer instance, Matrix4fc modelViewMatrix, boolean b, Operation<ChunkSectionsToRender> original, @Local Vector4f fogColor) {
        var projectionMatrix = ((GameRendererStorage) Minecraft.getInstance().gameRenderer)
                .sodium$getProjectionMatrix();

        this.matrices = new ChunkRenderMatrices(projectionMatrix, modelViewMatrix);
        var pos = this.levelRenderState.cameraRenderState.pos;

        // update the fog color here with the actual fog color being used to render the sky, since the fog color that
        // SodiumWorldRenderer still has stored from FogRendererMixin is outdated.
        this.renderer.updateFogColor(fogColor);

        this.renderer.prepareChunkRendering(this.matrices, pos.x, pos.y, pos.z);
        return new SodiumChunkSection(renderer, matrices, pos.x, pos.y, pos.z);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean isSectionCompiledAndVisible(BlockPos pos, final long chunkFadeDuration) {
        return this.renderer.isSectionReady(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "invalidateCompiledGeometry", at = @At("HEAD"), cancellable = true)
    private void sodium$replace(ClientLevel level,
                                Options options,
                                Camera camera,
                                BlockColors blockColors,
                                CallbackInfo ci) {
        ci.cancel();

        this.cloudRenderer.markForRebuild();
        LeavesBlock.setCutoutLeaves(options.cutoutLeaves().get());

        this.renderer.reload();

        this.sectionRenderDispatcher = new IgnoringSectionRenderDispatcher(Util.backgroundExecutor(),
                this.renderBuffers,
                null,
                this.sectionOcclusionGraph::schedulePropagationFrom);
        this.viewArea = new IgnoringViewArea(this.sectionRenderDispatcher);
        this.sectionOcclusionGraph .waitAndReset(this.viewArea);

        this.clearVisibleSections();
    }

    /**
     * @reason Allow control of the texture filtering mode
     * @author pajic
     */
    @Redirect(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/mojang/renderpearl/api/textures/FilterMode;LINEAR:Lcom/mojang/renderpearl/api/textures/FilterMode;",
                    opcode = Opcodes.GETSTATIC))
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
