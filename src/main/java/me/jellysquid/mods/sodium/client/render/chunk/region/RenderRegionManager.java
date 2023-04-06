package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final StagingBuffer stagingBuffer;

    public RenderRegionManager(CommandList commandList) {
        this.stagingBuffer = createStagingBuffer(commandList);
    }

    public void cleanup() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            Iterator<RenderRegion> it = this.regions.values()
                    .iterator();

            while (it.hasNext()) {
                RenderRegion region = it.next();

                if (region.isEmpty()) {
                    region.delete(commandList);

                    it.remove();
                }
            }
        }
    }

    public void upload(CommandList commandList, Iterator<ChunkBuildResult> queue) {
        for (Map.Entry<RenderRegion, List<ChunkBuildResult>> entry : this.setupUploadBatches(commandList, queue).entrySet()) {
            RenderRegion region = entry.getKey();
            List<ChunkBuildResult> uploadQueue = entry.getValue();

            this.upload(commandList, region, uploadQueue);

            for (ChunkBuildResult result : uploadQueue) {
                result.render.onBuildFinished(result);

                result.delete();
            }
        }
    }

    private void upload(CommandList commandList, RenderRegion region, List<ChunkBuildResult> results) {
        List<PendingSectionUpload> sectionUploads = new ArrayList<>();

        for (ChunkBuildResult result : results) {
            for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
                var storage = region.getStorage(pass);

                if (storage != null) {
                    var graphics = storage.setState(result.render, null);

                    // De-allocate all storage for data we're about to replace
                    // This will allow it to be cheaply re-allocated just below
                    if (graphics != null) {
                        graphics.delete();
                    }
                }

                ChunkMeshData meshData = result.getMesh(pass);

                if (meshData != null) {
                    PendingUpload indexUpload = null;
                    if (pass.isReverseOrder() && meshData.quadCount != 0) {
                        //We want to keep it aligned to 24 bytes aligned so we can sort mixed index types
                        NativeBuffer buffer = new NativeBuffer(meshData.quadCount * 24);
                        var db = MemoryUtil.memAddress(buffer.getDirectBuffer());
                        SharedQuadIndexBuffer.IndexType.INTEGER.createIndexBuffer(db, meshData.quadCount);
                        indexUpload = new PendingUpload(buffer);
                    }
                    sectionUploads.add(new PendingSectionUpload(result.render, meshData, pass, new PendingUpload(meshData.getVertexData()), indexUpload));
                }
            }
        }

        // If we have nothing to upload, abort!
        if (sectionUploads.isEmpty()) {
            return;
        }

        boolean bufferChanged = region.vertexBuffers.upload(commandList, sectionUploads.stream().map(i -> i.vertexUpload));
        bufferChanged |= region.indexBuffers.upload(commandList, sectionUploads.stream().map(i -> i.indicesUpload).filter(Objects::nonNull));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (bufferChanged) {
            region.deleteTessellations(commandList);
        }

        // Collect the upload results
        for (PendingSectionUpload upload : sectionUploads) {
            region.createStorage(upload.pass)
                    .replaceState(upload.section, new ChunkGraphicsState(upload.vertexUpload.getResult(), upload.indicesUpload!=null?upload.indicesUpload.getResult():null, upload.meshData));
            if (upload.indicesUpload != null) {
                //Delete the uploading index buffer
                upload.indicesUpload.getDataBuffer().free();
            }
        }
    }

    private Map<RenderRegion, List<ChunkBuildResult>> setupUploadBatches(CommandList commandList, Iterator<ChunkBuildResult> renders) {
        Map<RenderRegion, List<ChunkBuildResult>> map = new Reference2ObjectLinkedOpenHashMap<>();

        while (renders.hasNext()) {
            ChunkBuildResult result = renders.next();
            RenderSection render = result.render;

            if (!render.canAcceptBuildResults(result)) {
                result.delete();

                continue;
            }

            RenderRegion region = this.prepareRegionForChunk(commandList, render.getChunkX(), render.getChunkY(), render.getChunkZ());

            List<ChunkBuildResult> uploadQueue = map.computeIfAbsent(region, k -> new ArrayList<>());
            uploadQueue.add(result);
        }

        return map;
    }

    public RenderRegion prepareRegionForChunk(CommandList commandList, int x, int y, int z) {
        long key = RenderRegion.getRegionKeyForChunk(x, y, z);
        RenderRegion region = this.regions.get(key);

        if (region == null) {
            this.regions.put(key, region = RenderRegion.createRegionForChunk(commandList, this.stagingBuffer, x, y, z));
        }

        return region;
    }

    public void delete(CommandList commandList) {
        for (RenderRegion region : this.regions.values()) {
            region.delete(commandList);
        }

        this.regions.clear();
        this.stagingBuffer.delete(commandList);
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    public StagingBuffer getStagingBuffer() {
        return this.stagingBuffer;
    }

    private static StagingBuffer createStagingBuffer(CommandList commandList) {
        if (SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            return new MappedStagingBuffer(commandList);
        }

        return new FallbackStagingBuffer(commandList);
    }

    public RenderRegion getRegion(long longKey) {
        return this.regions.get(longKey);
    }

    private record PendingSectionUpload(RenderSection section, ChunkMeshData meshData, TerrainRenderPass pass, PendingUpload vertexUpload, PendingUpload indicesUpload) {
    }
}
