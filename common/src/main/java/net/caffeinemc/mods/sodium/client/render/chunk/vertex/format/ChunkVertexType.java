package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format;

import com.mojang.renderpearl.api.vertex.VertexFormat;

public interface ChunkVertexType {
    VertexFormat getVertexFormat();

    ChunkVertexEncoder getEncoder();
}
