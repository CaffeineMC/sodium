package net.caffeinemc.mods.sodium.client.gl.state;

import net.caffeinemc.mods.sodium.client.gl.array.GlVertexArray;
import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferTarget;

import java.util.Arrays;

public class GlStateTracker {
    private static final int UNASSIGNED_HANDLE = -1;

    private int vertexArrayState;

    public GlStateTracker() {

    }

    public void notifyVertexArrayDeleted(GlVertexArray vertexArray) {
        if (this.vertexArrayState == vertexArray.handle()) {
            this.vertexArrayState = UNASSIGNED_HANDLE;
        }
    }

}
