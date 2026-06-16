package net.caffeinemc.mods.sodium.mixin.core.render.world;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.viewport.ViewportProvider;
import net.caffeinemc.mods.sodium.client.util.FlawlessFrames;
import net.caffeinemc.mods.sodium.client.util.FogStorage;
import net.caffeinemc.mods.sodium.client.world.LevelRendererExtension;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LevelExtractor.class)
public class LevelExtractorMixin {
    @Shadow
    private @Nullable ClientLevel level;

    @Unique
    private SodiumWorldRenderer renderer;

    @Inject(method = "extract", at = @At("HEAD"))
    private void sodium$setRenderer(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        checkRenderer();
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void sodium$setRenderer(ClientLevel level, CallbackInfo ci) {
        checkRenderer();

        this.renderer.setLevel(level);
    }

    @Unique
    private void checkRenderer() {
        this.renderer = ((LevelRendererExtension) Minecraft.getInstance().levelRenderer).sodium$getWorldRenderer();
    }

    @Inject(method = "extractVisibleBlockEntities", at = @At("HEAD"), cancellable = true, require = 1)
    private void extractVisibleBlockEntities(Camera camera, float f, LevelRenderState levelRenderState, CallbackInfo ci) {
        ci.cancel();

        this.renderer.extractBlockEntities(camera, f, this.level.destructionProgress(), levelRenderState);
    }

    // Exclusive to NeoForge, allow to fail.
    @SuppressWarnings("all")
    @Inject(method = "iterateVisibleBlockEntities", at = @At("HEAD"), cancellable = true, expect = 0, require = 0)
    public void replaceBlockEntityIteration(Consumer<BlockEntity> blockEntityConsumer, CallbackInfo ci) {
        ci.cancel();

        this.renderer.iterateVisibleBlockEntities(blockEntityConsumer);
    }


    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String sectionStatistics() {
        checkRenderer();
        return this.renderer.getChunksDebugString();
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        checkRenderer();
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setSectionDirtyWithNeighbors(int x, int y, int z) {
        checkRenderer();
        this.renderer.scheduleRebuildForChunks(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setBlockDirty(BlockPos pos, boolean important) {
        checkRenderer();
        this.renderer.scheduleRebuildForBlockArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, important);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean important) {
        checkRenderer();
        this.renderer.scheduleRebuildForChunk(x, y, z, important);
    }
    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Inject(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionOcclusionGraph;consumeFrustumUpdate()Z"))
    private void cullTerrain(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci, @Local Frustum frustum) {
        var viewport = ((ViewportProvider) frustum).sodium$createViewport();
        var updateChunksImmediately = FlawlessFrames.isActive();

        this.renderer.setupTerrain(camera, viewport, ((FogStorage) Minecraft.getInstance().gameRenderer).sodium$getFogParameters(), Minecraft.getInstance().player != null && Minecraft.getInstance().player.isSpectator(), updateChunksImmediately, ((FrustumAccessor) frustum).sodium$getMatrix());
    }

    @Redirect(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;applyFrustum(Lnet/minecraft/client/renderer/culling/Frustum;)V"))
    private void sodium$cancel(LevelExtractor instance, Frustum frustum) {}

    /**
     * @reason Redirect to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int countRenderedSections() {
        checkRenderer();
        return this.renderer.getVisibleChunkCount();
    }

}
