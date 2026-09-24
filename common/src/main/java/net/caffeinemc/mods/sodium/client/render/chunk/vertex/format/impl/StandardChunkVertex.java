package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;

public class StandardChunkVertex implements ChunkVertexType {
    public static final int STRIDE = 28;

    public static final VertexFormat VERTEX_FORMAT = VertexFormat.builder(0)
            .addAttribute("a_Position", GpuFormat.RGB32_FLOAT)
            .addAttribute("a_Color", GpuFormat.RGBA8_UNORM)
            .addAttribute("a_TexCoord", GpuFormat.RG32_FLOAT)
            .addAttribute("a_LightAndData", GpuFormat.RGBA8_UINT).build();

    @Override
    public VertexFormat getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, materialBits, vertices, section) -> {
            for (int i = 0; i < 4; i++) {
                var vertex = vertices[i];

                int light = encodeLight(vertex.light);

                MemoryIntrinsics.putFloat(ptr +  0L, vertex.x);
                MemoryIntrinsics.putFloat(ptr +  4L, vertex.y);
                MemoryIntrinsics.putFloat(ptr +  8L, vertex.z);
                MemoryIntrinsics.putInt(ptr + 12L, ColorARGB.mulRGB(vertex.color, vertex.ao));
                MemoryIntrinsics.putFloat(ptr + 16L, vertex.u);
                MemoryIntrinsics.putFloat(ptr + 20L, vertex.v);
                MemoryIntrinsics.putInt(ptr + 24L, packLightAndData(light, materialBits, section));

                ptr += STRIDE;
            }

            return ptr;
        };
    }

    private static int encodeLight(int light) {
        int sky = (light >>> 16) & 0xFF;
        int block = (light >>> 0) & 0xFF;

        return (block << 0) | (sky << 8);
    }

    private static int packLightAndData(int light, int material, int section) {
        return ((light & 0xFFFF) << 0) |
                ((material & 0xFF) << 16) |
                ((section & 0xFF) << 24);
    }
}