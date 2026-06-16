package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format;

import com.mojang.blaze3d.vertex.VertexFormat;

public interface ChunkVertexType {
    VertexFormat getVertexFormat();

    ChunkVertexEncoder getEncoder();
}
