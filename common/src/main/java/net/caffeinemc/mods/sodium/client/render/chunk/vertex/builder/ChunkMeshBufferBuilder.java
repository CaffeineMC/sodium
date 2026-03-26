package net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import org.lwjgl.system.MemoryUtil;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

public class ChunkMeshBufferBuilder {
    private final ChunkVertexEncoder encoder;
    private final int stride;

    private final int initialCapacity;

    private MemorySegment buffer;
    private int vertexCount;
    private int vertexCapacity;

    private int sectionIndex;

    public ChunkMeshBufferBuilder(ChunkVertexType vertexType, int initialCapacity) {
        this.encoder = vertexType.getEncoder();
        this.stride = vertexType.getVertexFormat().getStride();

        this.buffer = null;

        this.vertexCapacity = initialCapacity;
        this.initialCapacity = initialCapacity;
    }

    public void push(ChunkVertexEncoder.Vertex[] vertices, Material material) {
        this.push(vertices, material.bits());
    }

    public void push(ChunkVertexEncoder.Vertex[] vertices, int materialBits) {
        if (vertices.length != 4) {
            throw new IllegalArgumentException("Only quad primitives (with 4 vertices) can be pushed");
        }

        this.ensureCapacity(4);

        this.encoder.write(this.buffer.address() + ((long) this.vertexCount * this.stride),
                materialBits, vertices, this.sectionIndex);
        this.vertexCount += 4;
    }

    public void writeExternal(ByteBuffer buffer, int position, ChunkVertexEncoder.Vertex[] vertices, Material material) {
        this.encoder.write(MemoryUtil.memAddress(buffer, position * this.stride),
                material.bits(), vertices, this.sectionIndex);
    }

    private void ensureCapacity(int vertexCount) {
        if (this.vertexCount + vertexCount >= this.vertexCapacity) {
            this.grow(vertexCount);
        }
    }

    private void grow(int vertexCount) {
        this.reallocate(
                // The new capacity will at least twice as large
                Math.max(this.vertexCapacity * 2, this.vertexCapacity + vertexCount)
        );
    }

    private void reallocate(int vertexCount) {
        this.buffer = MemorySegment.ofAddress(MemoryUtil.nmemRealloc(this.buffer == null ? 0L : this.buffer.address(), vertexCount * this.stride)).reinterpret(vertexCount * this.stride);
        this.vertexCapacity = vertexCount;
    }

    public void start(int sectionIndex) {
        this.vertexCount = 0;
        this.sectionIndex = sectionIndex;

        this.reallocate(this.initialCapacity);
    }

    public void destroy() {
        if (this.buffer != null) {
            MemoryUtil.nmemFree(this.buffer.address());
        }

        this.buffer = null;
    }

    public boolean isEmpty() {
        return this.vertexCount == 0;
    }

    public ByteBuffer slice() {
        if (this.isEmpty()) {
            throw new IllegalStateException("No vertex data in buffer");
        }

        return this.buffer.asSlice(0, this.stride * this.vertexCount).asByteBuffer();
    }

    public int count() {
        return this.vertexCount;
    }
}
