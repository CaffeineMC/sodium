package net.caffeinemc.mods.sodium.client.gpu.device.backend;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vulkan.VulkanDevice;
import net.caffeinemc.mods.sodium.mixin.core.GpuDeviceAccessor;

public enum DrawBackend {
    OPENGL,
    VK_MULTIDRAW,
    VK_INDIRECT;

    public static final DrawBackend BACKEND = chooseBackend();

    private static DrawBackend chooseBackend() {
        GpuDevice device = RenderSystem.getDevice();

        if (((GpuDeviceAccessor) device).sodium$getBackend() instanceof VulkanDevice) {
            if (device.getDeviceInfo().features().multiDrawDirectInterleaved()) {
                return DrawBackend.VK_MULTIDRAW;
            } else if (device.getDeviceInfo().features().multiDrawIndirect()) {
                return DrawBackend.VK_INDIRECT;
            } else {
                throw new IllegalStateException("Somehow, none of the multidraw backends are supported by this device.");
            }
        } else {
            return DrawBackend.OPENGL;
        }
    }
}
