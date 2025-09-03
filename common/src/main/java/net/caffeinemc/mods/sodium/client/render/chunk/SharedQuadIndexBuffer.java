package net.caffeinemc.mods.sodium.client.render.chunk;

import net.caffeinemc.mods.sodium.client.gl.buffer.GlBuffer;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferMapFlags;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferUsage;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlMutableBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.tessellation.GlIndexType;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class SharedQuadIndexBuffer {
    /**
     * Index layout for triangle strips per quad using primitive restart:
     * For each quad i, write 4 indices [4*i + 0, 4*i + 1, 4*i + 2, 4*i + 3]. Between adjacent quads,
     * write one primitive restart index (0xFFFF for UNSIGNED_SHORT, 0xFFFFFFFF for UNSIGNED_INT).
     * The final quad omits the trailing restart.
     * For q quads, total index count is (4*q) + (q - 1) = 5*q - 1.
     */
    private static final int INDICES_PER_QUAD_WITH_RESTART_SLOT = 5;
    private static final int VERTICES_PER_PRIMITIVE = 4;

    private final GlMutableBuffer buffer;
    private final IndexType indexType;

    private int maxPrimitives;

    public SharedQuadIndexBuffer(CommandList commandList, IndexType indexType) {
        this.buffer = commandList.createMutableBuffer();
        this.indexType = indexType;
    }

    public void ensureCapacity(CommandList commandList, int elementCount) {
        if (elementCount > this.indexType.getMaxElementCount()) {
            throw new IllegalArgumentException("Tried to reserve storage for more indices in this buffer than it can hold");
        }

        // For triangle strips: elementCount = 5*q - 1  =>  q = ceil((elementCount + 1) / 5)
        int primitiveCount = (elementCount + 1 + INDICES_PER_QUAD_WITH_RESTART_SLOT - 1) / INDICES_PER_QUAD_WITH_RESTART_SLOT;

        if (primitiveCount > this.maxPrimitives) {
            this.grow(commandList, this.getNextSize(primitiveCount));
        }
    }

    private int getNextSize(int primitiveCount) {
        return Math.min(Math.max(this.maxPrimitives * 2, primitiveCount + 16384), this.indexType.getMaxPrimitiveCount());
    }

    private void grow(CommandList commandList, int primitiveCount) {
        int indexCount = primitiveCount * INDICES_PER_QUAD_WITH_RESTART_SLOT - 1; // omit trailing restart
        long bufferSize = (long) indexCount * this.indexType.getBytesPerElement();

        commandList.allocateStorage(this.buffer, bufferSize, GlBufferUsage.STATIC_DRAW);

        var mapped = commandList.mapBuffer(this.buffer, 0, bufferSize, EnumBitField.of(GlBufferMapFlags.INVALIDATE_BUFFER, GlBufferMapFlags.WRITE, GlBufferMapFlags.UNSYNCHRONIZED));
        this.indexType.createIndexBuffer(mapped.getMemoryBuffer(), primitiveCount);

        commandList.unmap(mapped);

        this.maxPrimitives = primitiveCount;
    }

    public static NativeBuffer createIndexBuffer(IndexType indexType, int primitiveCount) {
        int indexCount = primitiveCount * INDICES_PER_QUAD_WITH_RESTART_SLOT - 1; // omit trailing restart
        int bufferSize = indexCount * indexType.getBytesPerElement();
        var buffer = new NativeBuffer(bufferSize);

        indexType.createIndexBuffer(buffer.getDirectBuffer(), primitiveCount);

        return buffer;
    }

    public GlBuffer getBufferObject() {
        return this.buffer;
    }

    public void delete(CommandList commandList) {
        commandList.deleteBuffer(this.buffer);
    }

    public enum IndexType {
        SHORT(GlIndexType.UNSIGNED_SHORT, 64 * 1024) {
            @Override
            public void createIndexBuffer(ByteBuffer byteBuffer, int primitiveCount) {
                // Guard against overflow when using UNSIGNED_SHORT
                int maxIndex = primitiveCount * VERTICES_PER_PRIMITIVE - 1;
                if (maxIndex > 0xFFFF) {
                    throw new IllegalStateException("Too many vertices for UNSIGNED_SHORT index buffer; use UNSIGNED_INT instead");
                }

                ShortBuffer shortBuffer = byteBuffer.asShortBuffer();

                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                    int indexOffset = primitiveIndex * INDICES_PER_QUAD_WITH_RESTART_SLOT;
                    int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

                    shortBuffer.put(indexOffset, (short) (vertexOffset));
                    shortBuffer.put(indexOffset + 1, (short) (vertexOffset + 1));
                    shortBuffer.put(indexOffset + 2, (short) (vertexOffset + 3));
                    shortBuffer.put(indexOffset + 3, (short) (vertexOffset + 2));
                    if (primitiveIndex < primitiveCount - 1) {
                        shortBuffer.put(indexOffset + 4, (short) 0xFFFF); // Primitive restart
                    }
                }
            }
        },
        INTEGER(GlIndexType.UNSIGNED_INT, Integer.MAX_VALUE) {
            @Override
            public void createIndexBuffer(ByteBuffer byteBuffer, int primitiveCount) {
                IntBuffer intBuffer = byteBuffer.asIntBuffer();

                for (int primitiveIndex = 0; primitiveIndex < primitiveCount; primitiveIndex++) {
                    int indexOffset = primitiveIndex * INDICES_PER_QUAD_WITH_RESTART_SLOT;
                    int vertexOffset = primitiveIndex * VERTICES_PER_PRIMITIVE;

                    intBuffer.put(indexOffset, vertexOffset);
                    intBuffer.put(indexOffset + 1, vertexOffset + 1);
                    intBuffer.put(indexOffset + 2, vertexOffset + 3);
                    intBuffer.put(indexOffset + 3, vertexOffset + 2);
                    if (primitiveIndex < primitiveCount - 1) {
                        intBuffer.put(indexOffset + 4, 0xFFFFFFFF); // Primitive restart
                    }
                }
            }
        };

        public static final IndexType[] VALUES = IndexType.values();

        private final GlIndexType format;
        private final int maxElementCount;

        IndexType(GlIndexType format, int maxElementCount) {
            this.format = format;
            this.maxElementCount = maxElementCount;
        }

        public abstract void createIndexBuffer(ByteBuffer buffer, int primitiveCount);

        public int getBytesPerElement() {
            return this.format.getStride();
        }

        public GlIndexType getFormat() {
            return this.format;
        }

        public int getMaxPrimitiveCount() {
            return this.maxElementCount / INDICES_PER_QUAD_WITH_RESTART_SLOT;
        }

        public int getMaxElementCount() {
            return this.maxElementCount;
        }
    }
}
