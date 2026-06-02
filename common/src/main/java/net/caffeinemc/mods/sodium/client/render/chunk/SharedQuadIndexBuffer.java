package net.caffeinemc.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class SharedQuadIndexBuffer {
    private static final int ELEMENTS_PER_PRIMITIVE = 6;
    private static final int VERTICES_PER_PRIMITIVE = 4;

    private GpuBuffer buffer;
    private final IndexFormat indexFormat;

    private int maxPrimitives;

    public SharedQuadIndexBuffer(IndexFormat indexFormat) {
        this.indexFormat = indexFormat;
        ensureCapacity(1);
    }

    public void ensureCapacity(int elementCount) {
        if (elementCount > this.indexFormat.getMaxElementCount()) {
            throw new IllegalArgumentException("Tried to reserve storage for more vertices in this buffer than it can hold");
        }

        int primitiveCount = elementCount / ELEMENTS_PER_PRIMITIVE;

        if (primitiveCount > this.maxPrimitives) {
            this.grow(this.getNextSize(primitiveCount));
        }
    }

    private int getNextSize(int primitiveCount) {
        return Math.min(Math.max(this.maxPrimitives * 2, primitiveCount + 16384), this.indexFormat.getMaxPrimitiveCount());
    }

    private void grow(int primitiveCount) {
        if (this.buffer != null) this.buffer.close();

        var bufferSize = primitiveCount * this.indexFormat.getBytesPerElement() * ELEMENTS_PER_PRIMITIVE;

        this.buffer = RenderSystem.getDevice().createBuffer(() -> "Shared index buffer", GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE, bufferSize);

        var mapped = buffer.map(false, true);
        this.indexFormat.createIndexBuffer(mapped.data(), primitiveCount);

        mapped.close();

        this.maxPrimitives = primitiveCount;
    }

    public static NativeBuffer createIndexBuffer(IndexFormat indexFormat, int primitiveCount) {
        var bufferSize = primitiveCount * indexFormat.getBytesPerElement() * ELEMENTS_PER_PRIMITIVE;
        var buffer = new NativeBuffer(bufferSize);

        indexFormat.createIndexBuffer(buffer.getDirectBuffer(), primitiveCount);

        return buffer;
    }

    public GpuBuffer getBufferObject() {
        return this.buffer;
    }

    public void delete() {
        if (this.buffer != null) this.buffer.close();
    }

    public enum IndexFormat {
        SHORT(IndexType.SHORT, 64 * 1024) {
            @Override
            public void createIndexBuffer(ByteBuffer byteBuffer, int primitiveCount) {
                ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                    int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;
                    int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

                    shortBuffer.put(indexOffset + 0, (short) (vertexOffset + 0));
                    shortBuffer.put(indexOffset + 1, (short) (vertexOffset + 1));
                    shortBuffer.put(indexOffset + 2, (short) (vertexOffset + 2));

                    shortBuffer.put(indexOffset + 3, (short) (vertexOffset + 2));
                    shortBuffer.put(indexOffset + 4, (short) (vertexOffset + 3));
                    shortBuffer.put(indexOffset + 5, (short) (vertexOffset + 0));
                }
            }
        },
        INTEGER(IndexType.INT, Integer.MAX_VALUE) {
            @Override
            public void createIndexBuffer(ByteBuffer byteBuffer, int primitiveCount) {
                IntBuffer intBuffer = byteBuffer.asIntBuffer();

                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                    int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;
                    int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

                    intBuffer.put(indexOffset + 0, vertexOffset + 0);
                    intBuffer.put(indexOffset + 1, vertexOffset + 1);
                    intBuffer.put(indexOffset + 2, vertexOffset + 2);

                    intBuffer.put(indexOffset + 3, vertexOffset + 2);
                    intBuffer.put(indexOffset + 4, vertexOffset + 3);
                    intBuffer.put(indexOffset + 5, vertexOffset + 0);
                }
            }
        };

        public static final IndexFormat[] VALUES = IndexFormat.values();

        private final IndexType format;
        private final int maxElementCount;

        IndexFormat(IndexType format, int maxElementCount) {
            this.format = format;
            this.maxElementCount = maxElementCount;
        }

        public abstract void createIndexBuffer(ByteBuffer buffer, int primitiveCount);

        public int getBytesPerElement() {
            return this.format.bytes;
        }

        public IndexType getFormat() {
            return this.format;
        }

        public int getMaxPrimitiveCount() {
            return this.maxElementCount / 4;
        }

        public int getMaxElementCount() {
            return this.maxElementCount;
        }
    }
}
