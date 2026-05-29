package net.caffeinemc.mods.sodium.client.gl.device;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import net.caffeinemc.mods.sodium.mixin.core.GpuDeviceAccessor;
import org.lwjgl.PointerBuffer;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkMultiDrawIndexedInfoEXT;

import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch {
    private long pElementPointer;
    private long pElementCount;
    private long pBaseVertex;

    private long pCommands;

    public int size;
    public boolean isFilled;

    public MultiDrawBatch(int capacity) {
        if (((GpuDeviceAccessor) RenderSystem.getDevice()).sodium$getBackend() instanceof GlDevice) {
            this.pElementPointer = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Pointer.POINTER_SIZE);
            MemoryUtil.memSet(this.pElementPointer, 0x0, (long) capacity * Pointer.POINTER_SIZE);

            this.pElementCount = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
            this.pBaseVertex = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        } else {
            this.pCommands = MemoryUtil.nmemAlignedAlloc(32, (long) VkMultiDrawIndexedInfoEXT.SIZEOF * capacity);
            MemoryUtil.memSet(this.pCommands, 0x0, (long) VkMultiDrawIndexedInfoEXT.SIZEOF * capacity);
        }
    }

    public void clear() {
        this.size = 0;
        this.isFilled = false;
    }

    public void delete() {
        if (this.pCommands != 0) {
            MemoryUtil.nmemAlignedFree(this.pCommands);
        } else {
            MemoryUtil.nmemAlignedFree(this.pElementPointer);
            MemoryUtil.nmemAlignedFree(this.pElementCount);
            MemoryUtil.nmemAlignedFree(this.pBaseVertex);
        }
    }

    public boolean isEmpty() {
        return this.size <= 0;
    }

    public int getIndexBufferSize() {
        int elements = 0;

        if (pCommands != 0) {
            for (var index = 0; index < this.size; index++) {
                elements = Math.max(elements, MemoryIntrinsics.getInt(this.pCommands + ((long) index * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.INDEXCOUNT));
            }
        } else {
            for (var index = 0; index < this.size; index++) {
                elements = Math.max(elements, MemoryIntrinsics.getInt(this.pElementCount + ((long) index * Integer.BYTES)));
            }
        }

        return elements;
    }

    public void put(int size, int elementCount, int baseVertex, long elementOffset) {
        if (pCommands != 0) {
            MemoryIntrinsics.putInt(pCommands + (size * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.INDEXCOUNT, elementCount);
            MemoryIntrinsics.putInt(pCommands + (size * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.VERTEXOFFSET, UInt32.uncheckedDowncast(baseVertex));
            MemoryIntrinsics.putInt(pCommands + (size * VkMultiDrawIndexedInfoEXT.SIZEOF) + VkMultiDrawIndexedInfoEXT.FIRSTINDEX, UInt32.uncheckedDowncast(elementOffset));
        } else {
            MemoryIntrinsics.putInt(pElementCount + (size << 2), UInt32.uncheckedDowncast(elementCount));
            MemoryIntrinsics.putInt(pBaseVertex + (size << 2), UInt32.uncheckedDowncast(baseVertex));

            // * 4 to convert to bytes (the index buffer contains integers)
            MemoryIntrinsics.putAddress(pElementPointer + (size << Pointer.POINTER_SHIFT), elementOffset << 2);
        }
    }

    public void drawGL(RenderPass pass) {
        pass.multiDrawIndexed(MemoryUtil.memPointerBuffer(pElementPointer, size),
                MemoryUtil.memIntBuffer(pElementCount, size),
                MemoryUtil.memIntBuffer(pBaseVertex, size), size);
    }

    public void drawVK(RenderPass pass) {
        pass.multiDrawIndexed(MemoryUtil.memIntBuffer(pCommands, size * 3), 1, 0, size);
    }
}
