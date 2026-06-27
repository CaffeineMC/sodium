package net.caffeinemc.mods.sodium.client.gpu.arena;

import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public class PendingUpload {
    private final NativeBuffer data;
    private GlBufferSegment result;
    private final int segmentOwnerIndex;

    public PendingUpload(NativeBuffer data, int segmentOwnerIndex) {
        this.data = data;
        this.segmentOwnerIndex = segmentOwnerIndex;
    }

    public NativeBuffer getDataBuffer() {
        return this.data;
    }

    protected void setResult(GlBufferSegment result) {
        if (this.result != null) {
            throw new IllegalStateException("Result already provided");
        }

        this.result = result;
    }

    public GlBufferSegment getResult() {
        if (this.result == null) {
            throw new IllegalStateException("Result not computed");
        }

        return this.result;
    }

    public int getLength() {
        return this.data.getLength();
    }

    public int getSegmentOwnerIndex() {
        return this.segmentOwnerIndex;
    }
}
