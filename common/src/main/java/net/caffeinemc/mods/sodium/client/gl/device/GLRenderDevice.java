package net.caffeinemc.mods.sodium.client.gl.device;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.gl.functions.DeviceFunctions;
import net.caffeinemc.mods.sodium.client.gl.state.GlStateTracker;
import net.caffeinemc.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.opengl.*;
import java.nio.ByteBuffer;

public class GLRenderDevice implements RenderDevice {
    private final GlStateTracker stateTracker = new GlStateTracker();
    private final CommandList commandList = new ImmediateCommandList(this.stateTracker);
    private final DrawCommandList drawCommandList = new ImmediateDrawCommandList();

    private final DeviceFunctions functions = new DeviceFunctions(this);

    private boolean isActive;

    @Override
    public CommandList createCommandList() {
        GLRenderDevice.this.checkDeviceActive();

        return this.commandList;
    }

    @Override
    public void makeActive() {
        if (this.isActive) {
            return;
        }

        this.isActive = true;
    }

    @Override
    public void makeInactive() {
        if (!this.isActive) {
            return;
        }

        this.isActive = false;
    }

    @Override
    public GLCapabilities getCapabilities() {
        return null;
    }

    @Override
    public DeviceFunctions getDeviceFunctions() {
        return this.functions;
    }

    @Override
    public int getSubTexelPrecisionBits() {
        // OpenGL only specifies "at least" 4 bits of sub-texel precision for texture fetches. Thankfully, nearly every
        // graphics card is Direct3D-compatible and capable of providing 8 bits of precision. The only exception to this
        // rule seems to be when using OpenGL on macOS, where it appears to arbitrarily limit the precision to 4 bits
        // *even if* the hardware is capable of better.
        if (OsUtils.getOs() == OsUtils.OperatingSystem.MAC) {
            return 4;
        }

        return 8;
    }

    private void checkDeviceActive() {
        if (!this.isActive) {
            throw new IllegalStateException("Tried to access device from unmanaged context");
        }
    }

    private class ImmediateCommandList implements CommandList {
        private final GlStateTracker stateTracker;

        private ImmediateCommandList(GlStateTracker stateTracker) {
            this.stateTracker = stateTracker;
        }

        @Override
        public void flush() {
            // NO-OP
        }
        @Override
        public void flushMappedRange(GpuBufferSlice.MappedView map, int start, int i) {

        }
    }

    private class ImmediateDrawCommandList implements DrawCommandList {
        public ImmediateDrawCommandList() {

        }

        @Override
        public void endTessellating() {
        }

        @Override
        public void flush() {

        }
    }
}
