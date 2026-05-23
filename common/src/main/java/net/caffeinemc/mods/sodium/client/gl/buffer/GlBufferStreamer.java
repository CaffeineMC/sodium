package net.caffeinemc.mods.sodium.client.gl.buffer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.opengl.*;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import org.lwjgl.system.MemoryUtil;

public class GlBufferStreamer {
    private final GpuBuffer buffer;
    private final GpuBufferSlice.MappedView mapping;
    private final long writeAddress;

    private final int stride;
    private final long bufferSize;
    private boolean requiresFlush;

    public GlBufferStreamer(CommandList commands, int initialCapacity, int stride) {
        this.bufferSize = (long) initialCapacity * stride;
        this.stride = stride;

        this.buffer = RenderSystem.getDevice().createBuffer(() -> "Streamed buffer", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_WRITE, bufferSize);

        if (RenderSystem.getDevice().getDeviceInfo().features().persistentMapping()) {
            this.mapping = buffer.map(false, true);

            this.writeAddress = MemoryUtil.memAddress(this.mapping.data());
        } else {
            this.mapping = null;
            this.writeAddress = MemoryUtil.nmemAlloc(this.bufferSize);
        }

        MemoryUtil.memSet(this.writeAddress, (byte) 0, bufferSize); // without this, I was getting random chunks with no fade. TODO: Check if this is still needed after the mesh check improvements
    }

    public void writeData(int index, int value) { // right now we only need int values... this could probably become more generic (if we ever need this again?)
        int offset = index * stride;

        if (offset + stride > bufferSize) {
            throw new IndexOutOfBoundsException("Attempted to write beyond the end of the buffer streamer");
        }

        MemoryIntrinsics.putInt(this.writeAddress + offset, value);
        this.requiresFlush = true;
    }

    public GpuBuffer prepare(CommandList commandList) { // either flushes or uploads data. This could be replaced with a batching system, but I don't see the point with the tiny buffer we currently use it for.
        if (requiresFlush) {
            requiresFlush = false;
            if (this.mapping != null) {
                GL44C.glMemoryBarrier(GL44C.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT); // TODO: I don't know yet if this is required.
            } else {
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), MemoryUtil.memByteBuffer(writeAddress, (int) bufferSize));
            }
        }

        return buffer;
    }

    public void delete(CommandList commandList) {
        if (this.mapping != null) {
            this.mapping.close();
        } else {
            MemoryUtil.nmemFree(this.writeAddress);
        }

        this.buffer.close();
    }
}