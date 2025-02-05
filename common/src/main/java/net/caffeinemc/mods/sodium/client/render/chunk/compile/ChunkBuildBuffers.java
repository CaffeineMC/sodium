package net.caffeinemc.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.bsp_tree.BSPResult;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private final Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders = new Reference2ReferenceOpenHashMap<>();

    private final ChunkVertexType vertexType;

    public ChunkBuildBuffers(ChunkVertexType vertexType) {
        this.vertexType = vertexType;

        for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
            var vertexBuffers = new ChunkMeshBufferBuilder[ModelQuadFacing.COUNT];

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                vertexBuffers[facing] = new ChunkMeshBufferBuilder(this.vertexType, 128 * 1024);
            }

            this.builders.put(pass, new BakedChunkModelBuilder(vertexBuffers));
        }
    }

    public void init(BuiltSectionInfo.Builder renderData, int sectionIndex) {
        for (var builder : this.builders.values()) {
            builder.begin(renderData, sectionIndex);
        }
    }

    public ChunkModelBuilder get(Material material) {
        return this.builders.get(material.pass);
    }

    public ChunkModelBuilder get(TerrainRenderPass pass) {
        return this.builders.get(pass);
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public BuiltSectionMeshParts createMesh(TerrainRenderPass pass, boolean forceUnassigned) {
        var builder = this.builders.get(pass);

        List<ByteBuffer> vertexBuffers = new ArrayList<>();
        int[] vertexCounts = new int[ModelQuadFacing.COUNT];

        int vertexSum = 0;

        for (ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            var ordinal = facing.ordinal();
            var buffer = builder.getVertexBuffer(facing);

            if (buffer.isEmpty()) {
                continue;
            }

            vertexBuffers.add(buffer.slice());
            var bufferCount = buffer.count();
            if (!forceUnassigned) {
                vertexCounts[ordinal] = bufferCount;
            }

            vertexSum += bufferCount;
        }

        if (vertexSum == 0) {
            return null;
        }

        if (forceUnassigned) {
            vertexCounts[ModelQuadFacing.UNASSIGNED.ordinal()] = vertexSum;
        }

        var mergedBuffer = new NativeBuffer(vertexSum * this.vertexType.getVertexFormat().getStride());
        var mergedBufferBuilder = mergedBuffer.getDirectBuffer();

        for (var buffer : vertexBuffers) {
            mergedBufferBuilder.put(buffer);
        }

        return new BuiltSectionMeshParts(mergedBuffer, vertexCounts);
    }

    public BuiltSectionMeshParts createCompactModifiedTranslucentMesh(BuiltSectionMeshParts prevMesh, ChunkMeshBufferBuilder updateBufferBuilder, BSPResult.UpdatedQuadIndexes updatedQuadIndexes) {
        if (updateBufferBuilder.isEmpty()) {
            return null;
        }

//        var oldQuadCount = TranslucentData.vertexCountToQuadCount(prevMesh.getVertexCounts()[ModelQuadFacing.UNASSIGNED.ordinal()]);
//        var addedQuadCount = updatedQuadIndexes.getAddedQuadCount();
//        var totalNewQuadCount = oldQuadCount + addedQuadCount;
//        System.out.println("old quads: " + oldQuadCount + ", added quads: " + addedQuadCount + ", total quads: " + totalNewQuadCount + ", new/old factor: " + (float) totalNewQuadCount / oldQuadCount);

        var totalVertexCount = prevMesh.getVertexCounts()[ModelQuadFacing.UNASSIGNED.ordinal()] +
                TranslucentData.quadCountToVertexCount(updatedQuadIndexes.getAddedQuadCount());
        var vertexStride = this.vertexType.getVertexFormat().getStride();
        var mergedBuffer = new NativeBuffer(totalVertexCount * vertexStride);
        var quadStride = vertexStride * TranslucentData.VERTICES_PER_QUAD;
        var mergedBufferBuilder = mergedBuffer.getDirectBuffer();
        var updateBuffer = updateBufferBuilder.slice();

        // write the entire old mesh first, then apply modifications
        mergedBufferBuilder.put(prevMesh.getVertexData().getDirectBuffer());

        for (int sourceQuadIndex = 0; sourceQuadIndex < updatedQuadIndexes.size(); sourceQuadIndex++) {
            var targetQuadIndex = updatedQuadIndexes.getInt(sourceQuadIndex);
            mergedBufferBuilder.put(targetQuadIndex * quadStride, updateBuffer, sourceQuadIndex * quadStride, quadStride);
        }

        int[] vertexCounts = new int[ModelQuadFacing.COUNT];
        vertexCounts[ModelQuadFacing.UNASSIGNED.ordinal()] = totalVertexCount;
        return new BuiltSectionMeshParts(mergedBuffer, vertexCounts);
    }

    public void destroy() {
        for (var builder : this.builders.values()) {
            builder.destroy();
        }
    }
}
