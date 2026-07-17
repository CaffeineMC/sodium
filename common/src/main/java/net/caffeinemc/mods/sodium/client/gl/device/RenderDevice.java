package net.caffeinemc.mods.sodium.client.gl.device;

import net.caffeinemc.mods.sodium.client.gl.buffer.GlBufferTarget;
import net.caffeinemc.mods.sodium.client.gl.functions.DeviceFunctions;
import org.lwjgl.opengl.GLCapabilities;

public interface RenderDevice {
    RenderDevice INSTANCE = new GLRenderDevice();

    CommandList createCommandList();

    static void enterManagedCode() {
        RenderDevice.INSTANCE.makeActive();
    }

    static void exitManagedCode() {
        RenderDevice.INSTANCE.makeInactive();
    }

    void makeActive();
    void makeInactive();

    // Clears the cached binding of a GL buffer target so the next bind to it is not skipped. This must be called when a buffer is bound to this target by external unmanaged code.
    void invalidateBufferBinding(GlBufferTarget target);

    GLCapabilities getCapabilities();

    DeviceFunctions getDeviceFunctions();

    int getSubTexelPrecisionBits();
}
