package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.StandardChunkVertex;

public class ChunkMeshFormats {
    public static final ChunkVertexType COMPACT = new CompactChunkVertex();
    public static final ChunkVertexType STANDARD = new StandardChunkVertex();

    private static final boolean USE_COMPACT = true;

    public static ChunkVertexType getCurrent() {
        return USE_COMPACT ? COMPACT : STANDARD;
    }
}
