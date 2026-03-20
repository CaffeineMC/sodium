package net.caffeinemc.mods.sodium.client.gl.buffer;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.gl.device.RenderDevice;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

public class GlBufferStreamer {
    private final GlBuffer buffer;
    private final GlBufferMapping mapping;
    private final long writeAddress;

    private final int stride;
    private final long bufferSize;
    private boolean requiresFlush;

    public GlBufferStreamer(CommandList commands, int initialCapacity, int stride) {
        this.bufferSize = (long) initialCapacity * stride;
        this.stride = stride;

        if (SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE) && (GL.getCapabilities().GL_ARB_shader_image_load_store && GL.getCapabilities().glMemoryBarrier != 0L)) {
            this.buffer = commands.createImmutableBuffer(bufferSize, EnumBitField.of(GlBufferStorageFlags.PERSISTENT, GlBufferStorageFlags.MAP_WRITE));

            this.mapping = commands.mapBuffer(this.buffer, 0, bufferSize,
                    EnumBitField.of(GlBufferMapFlags.PERSISTENT, GlBufferMapFlags.WRITE, GlBufferMapFlags.EXPLICIT_FLUSH));
            this.writeAddress = MemoryUtil.memAddress(this.mapping.getMemoryBuffer());
        } else {
            this.buffer = commands.createMutableBuffer();
            commands.allocateStorage((GlMutableBuffer) this.buffer, bufferSize, GlBufferUsage.STREAM_DRAW);

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

        MemoryUtil.memPutInt(this.writeAddress + offset, value);
        this.requiresFlush = true;
    }

    public GlBuffer prepare(CommandList commandList) { // either flushes or uploads data. This could be replaced with a batching system, but I don't see the point with the tiny buffer we currently use it for.
        if (requiresFlush) {
            requiresFlush = false;
            if (this.mapping != null) {
                commandList.flushMappedRange(mapping, 0, (int) bufferSize);
                GL44C.glMemoryBarrier(GL44C.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT); // TODO: I don't know yet if this is required.
            } else {
                commandList.uploadDataToOffset((GlMutableBuffer) buffer, 0, writeAddress, (int) bufferSize);
            }
        }

        return buffer;
    }

    public void delete(CommandList commandList) {
        if (this.mapping != null) {
            commandList.unmap(this.mapping);
        } else {
            MemoryUtil.nmemFree(this.writeAddress);
        }

        commandList.deleteBuffer(this.buffer);
    }
}