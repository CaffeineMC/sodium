package net.caffeinemc.mods.sodium.client.render.vertex.buffer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;

public interface BufferBuilderExtension extends VertexBufferWriter {
    void sodium$duplicateVertex();

    /**
     * @return the vertex format this buffer builder is currently configured to write
     */
    VertexFormat sodium$getVertexFormat();

    @Override
    default boolean canUseIntrinsics(VertexFormat format) {
        return this.sodium$getVertexFormat() == format;
    }
}
