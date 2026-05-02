package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import org.jspecify.annotations.NonNull;

public interface GraphicsAdapterInfo {
    @NonNull GraphicsAdapterVendor vendor();

    @NonNull String name();

    record LinuxPciAdapterInfo(
            @NonNull GraphicsAdapterVendor vendor,
            @NonNull String name,

            String pciVendorId,
            String pciDeviceId
    ) implements GraphicsAdapterInfo {

    }
}