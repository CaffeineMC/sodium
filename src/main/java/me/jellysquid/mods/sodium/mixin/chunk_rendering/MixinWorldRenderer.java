package me.jellysquid.mods.sodium.mixin.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderDataVAO;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRendererVAO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    private ChunkRenderManager<ChunkRenderDataVAO> chunkManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftClient client, BufferBuilderStorage bufferBuilders, CallbackInfo ci) {
        this.chunkManager = new ChunkRenderManager<>(client, new ChunkRendererVAO());
    }

    @Inject(method = "setWorld", at = @At("RETURN"))
    private void onWorldChanged(ClientWorld world, CallbackInfo ci) {
        this.chunkManager.setWorld(world);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public int getCompletedChunkCount() {
        return this.chunkManager.getCompletedChunkCount();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void updateChunks(long limitTime) {
        this.chunkManager.updateChunks(limitTime);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    public boolean isTerrainRenderComplete() {
        return this.chunkManager.isTerrainRenderComplete();
    }

    @Inject(method = "scheduleTerrainUpdate", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.chunkManager.scheduleTerrainUpdate();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderLayer(RenderLayer renderLayer, MatrixStack matrixStack, double d, double e, double f) {
        this.chunkManager.renderLayer(renderLayer, matrixStack, d, e, f);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void renderChunkDebugInfo(Camera camera) {
        this.chunkManager.renderChunkDebugInfo(camera);
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.chunkManager.update(camera, frustum, hasForcedFrustum, frame, spectator);
    }

    @Inject(method = "clearChunkRenderers", at = @At("RETURN"))
    private void onChunkRenderersCleared(CallbackInfo ci) {
        this.chunkManager.clearRenderers();
    }

    @Inject(method = "reload", at = @At("RETURN"))
    private void reload(CallbackInfo ci) {
        this.chunkManager.reload();
    }

    /**
     * @author JellySquid
     */
    @Overwrite
    private void scheduleChunkRender(int x, int y, int z, boolean important) {
        this.chunkManager.scheduleRebuildForBlock(x, y, z, important);
    }
}
