package net.caffeinemc.mods.sodium.mixin.core;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FrontendGpuDevice.class)
public interface GpuDeviceAccessor {
    @Accessor("backend")
    GpuDeviceBackend sodium$getBackend();
}
