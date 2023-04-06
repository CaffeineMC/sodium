package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

import java.util.Map;

public class ChunkMeshData {
    private final Map<ModelQuadFacing, VertexRange> parts;
    private final NativeBuffer buffer;
    public final int quadCount;

    public ChunkMeshData(NativeBuffer buffer, Map<ModelQuadFacing, VertexRange> parts, int quadCount) {
        this.parts = parts;
        this.buffer = buffer;
        this.quadCount = quadCount;
    }

    public Map<ModelQuadFacing, VertexRange> getParts() {
        return this.parts;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }
}
