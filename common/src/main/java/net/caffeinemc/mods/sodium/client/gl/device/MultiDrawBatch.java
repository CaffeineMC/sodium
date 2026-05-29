package net.caffeinemc.mods.sodium.client.gl.device;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.util.UInt32;
import net.caffeinemc.mods.sodium.mixin.core.GpuDeviceAccessor;
import org.lwjgl.PointerBuffer;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkDrawIndexedIndirectCommand;
import org.lwjgl.vulkan.VkMultiDrawIndexedInfoEXT;

import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch {
    public static final long SIZEOFCOMMAND;
    private static final long INDEXCOUNT;
    private static final long FIRSTINDEX;
    private static final long VERTEXOFFSET;
    private static final long INSTANCECOUNT;
    private static final long BASEINSTANCE;
    private long pElementPointer;
    private long pElementCount;
    private long pBaseVertex;

    private long pCommands;

    public int size;
    public boolean isFilled;

    public static final boolean supportsMultiDraw;
    private static final boolean supportsIndirect;

    static {
        supportsMultiDraw = RenderSystem.getDevice().getDeviceInfo().features().multiDrawDirectInterleaved() || RenderSystem.getDevice().getDeviceInfo().features().multiDrawDirectSeparate();
        supportsIndirect = RenderSystem.getDevice().getDeviceInfo().features().multiDrawIndirect();
        SIZEOFCOMMAND = supportsMultiDraw ? VkMultiDrawIndexedInfoEXT.SIZEOF : VkDrawIndexedIndirectCommand.SIZEOF;
        INDEXCOUNT = supportsMultiDraw ? VkMultiDrawIndexedInfoEXT.INDEXCOUNT : VkDrawIndexedIndirectCommand.INDEXCOUNT;
        FIRSTINDEX = supportsMultiDraw ? VkMultiDrawIndexedInfoEXT.FIRSTINDEX : VkDrawIndexedIndirectCommand.FIRSTINDEX;
        VERTEXOFFSET = supportsMultiDraw ? VkMultiDrawIndexedInfoEXT.VERTEXOFFSET : VkDrawIndexedIndirectCommand.VERTEXOFFSET;
        INSTANCECOUNT = supportsMultiDraw ? -1 : VkDrawIndexedIndirectCommand.INSTANCECOUNT;
        BASEINSTANCE = supportsMultiDraw ? -1 : VkDrawIndexedIndirectCommand.FIRSTINSTANCE;
        if (!supportsMultiDraw && !supportsIndirect) {
            throw new IllegalStateException("This device does not support features required by Sodium. (Any way to do multidraw...)");
        }
    }

    public MultiDrawBatch(int capacity) {
        if (((GpuDeviceAccessor) RenderSystem.getDevice()).sodium$getBackend() instanceof GlDevice) {
            this.pElementPointer = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Pointer.POINTER_SIZE);
            MemoryUtil.memSet(this.pElementPointer, 0x0, (long) capacity * Pointer.POINTER_SIZE);

            this.pElementCount = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
            this.pBaseVertex = MemoryUtil.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        } else {
            this.pCommands = MemoryUtil.nmemAlignedAlloc(32, (long) SIZEOFCOMMAND * capacity);
            MemoryUtil.memSet(this.pCommands, 0x0, (long) SIZEOFCOMMAND * capacity);
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
                elements = Math.max(elements, MemoryIntrinsics.getInt(this.pCommands + ((long) index * SIZEOFCOMMAND) + INDEXCOUNT));
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
            MemoryIntrinsics.putInt(pCommands + (size * SIZEOFCOMMAND) + INDEXCOUNT, elementCount);
            MemoryIntrinsics.putInt(pCommands + (size * SIZEOFCOMMAND) + VERTEXOFFSET, UInt32.uncheckedDowncast(baseVertex));
            MemoryIntrinsics.putInt(pCommands + (size * SIZEOFCOMMAND) + FIRSTINDEX, UInt32.uncheckedDowncast(elementOffset));
            if (!supportsMultiDraw) {
                MemoryIntrinsics.putInt(pCommands + (size * SIZEOFCOMMAND) + INSTANCECOUNT, 1);
                MemoryIntrinsics.putInt(pCommands + (size * SIZEOFCOMMAND) + BASEINSTANCE, 0);
            }
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

    public int drawVK(int offsetInBuffer, GpuBufferSlice.MappedView transientMem, RenderPass pass) {
        if (supportsMultiDraw) {
            pass.multiDrawIndexed(MemoryUtil.memIntBuffer(pCommands, size * 3), 1, 0, size);
            return 0;
        } else {
            MemoryUtil.memCopy(pCommands, MemoryUtil.memAddress(transientMem.data()) + (offsetInBuffer * SIZEOFCOMMAND), size * SIZEOFCOMMAND);
            pass.drawIndexedIndirect(transientMem.slice().slice((offsetInBuffer * SIZEOFCOMMAND), size * SIZEOFCOMMAND), size);
            return size;
        }
    }
}
