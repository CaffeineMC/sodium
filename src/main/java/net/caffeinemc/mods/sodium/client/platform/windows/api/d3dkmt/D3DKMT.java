package net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt;

import com.sun.jna.platform.win32.VersionHelpers;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterInfo;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterVendor;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsFileVersion;
import net.caffeinemc.mods.sodium.client.platform.windows.api.Gdi32;
import net.caffeinemc.mods.sodium.client.platform.windows.api.version.Version;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.caffeinemc.mods.sodium.client.platform.windows.api.Gdi32.*;
import static net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMTQueryAdapterInfoType.WDDM12.*;
import static net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMTQueryAdapterInfoType.WDDM20.KMTQAITYPE_PHYSICALADAPTERDEVICEIDS;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memByteBuffer;

public class D3DKMT {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-D3DKMT");

    private static final boolean SUPPORTS_D3DKMT = VersionHelpers.IsWindowsVistaOrGreater() && Gdi32.isD3DKMTSupported();
    private static final boolean SUPPORTS_QUERYING_ADAPTER_TYPE = VersionHelpers.IsWindows8OrGreater();
    private static final boolean SUPPORTS_QUERYING_PCI_INFO = VersionHelpers.IsWindows10OrGreater();

    public static List<WDDMAdapterInfo> findGraphicsAdapters() {
        if (!SUPPORTS_D3DKMT) {
            LOGGER.warn("Unable to query graphics adapters when the operating system is older than Windows Vista.");
            return List.of();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var adapters = D3DKMTEnumAdaptersStruct.calloc(stack);
            apiCheckError("D3DKMTEnumAdapters", nD3DKMTEnumAdapters(adapters.address()));

            final D3DKMTAdapterInfoStruct.Buffer adapterInfoBuffer = adapters.getAdapters();

            try {
                return queryAdapters(adapterInfoBuffer);
            } finally {
                freeAdapters(adapterInfoBuffer);
            }
        }
    }

    private static @NotNull ArrayList<WDDMAdapterInfo> queryAdapters(@NotNull D3DKMTAdapterInfoStruct.Buffer adapterInfoBuffer) {
        var results = new ArrayList<WDDMAdapterInfo>();

        for (int adapterIndex = adapterInfoBuffer.position(); adapterIndex < adapterInfoBuffer.limit(); adapterIndex++) {
            var pAdapterInfo = adapterInfoBuffer.get(adapterIndex);
            int pAdapter = pAdapterInfo.getAdapterHandle();

            var parsed = getAdapterInfo(pAdapter, pAdapterInfo.getLuid());

            if (parsed != null) {
                results.add(parsed);
            }
        }

        return results;
    }

    private static void freeAdapters(@NotNull D3DKMTAdapterInfoStruct.Buffer adapterInfoBuffer) {
        for (int adapterIndex = adapterInfoBuffer.position(); adapterIndex < adapterInfoBuffer.limit(); adapterIndex++) {
            var adapterInfo = adapterInfoBuffer.get(adapterIndex);
            apiCheckError("D3DKMTCloseAdapter",
                    d3dkmtCloseAdapter(adapterInfo.getAdapterHandle()));
        }
    }

    private static @Nullable D3DKMT.WDDMAdapterInfo getAdapterInfo(int adapter, long luid) {
        int adapterType = -1;

        if (SUPPORTS_QUERYING_ADAPTER_TYPE) {
            adapterType = queryAdapterType(adapter);

            if (!isSupportedAdapterType(adapterType)) {
                return null;
            }
        }

        String adapterName = queryFriendlyName(adapter);

        @Nullable String driverFileName = queryDriverFileName(adapter);
        @Nullable WindowsFileVersion driverVersion = null;

        GraphicsAdapterVendor driverVendor = GraphicsAdapterVendor.UNKNOWN;

        if (driverFileName != null) {
            driverVersion = queryDriverVersion(driverFileName);
            driverVendor = GraphicsAdapterVendor.fromIcdName(getOpenGlIcdName(driverFileName));
        }

        PciInfo pciInfo = null;
        if (SUPPORTS_QUERYING_PCI_INFO) {
            pciInfo = queryPciInfo(adapter, 0);
        }

        return new WDDMAdapterInfo(driverVendor, adapterName, adapterType, luid, driverFileName, driverVersion, pciInfo);

    }

    private static boolean isSupportedAdapterType(int adapterType) {
        // Adapter does not support rendering
        if ((adapterType & 0x1) == 0) {
            return false;
        }

        // Adapter uses software rendering
        if ((adapterType & 0x4) != 0) {
            return false;
        }

        return true;
    }

    private static @Nullable String queryDriverFileName(int adapter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            D3DKMTOpenGLInfoStruct info = D3DKMTOpenGLInfoStruct.calloc(stack);
            d3dkmtQueryAdapterInfo(adapter, KMTQAITYPE_UMOPENGLINFO, memByteBuffer(info));

            return info.getUserModeDriverFileName();
        }
    }

    private static @Nullable WindowsFileVersion queryDriverVersion(String file) {
        var version = Version.getModuleFileVersion(file);

        if (version == null) {
            return null;
        }

        var fileVersion = version.queryFixedFileInfo();

        if (fileVersion == null) {
            return null;
        }

        return WindowsFileVersion.fromFileVersion(fileVersion);
    }

    private static @NotNull String queryFriendlyName(int adapter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            D3DKMTAdapterRegistryInfoStruct registryInfo = D3DKMTAdapterRegistryInfoStruct.calloc(stack);
            d3dkmtQueryAdapterInfo(adapter, KMTQAITYPE_ADAPTERREGISTRYINFO, memByteBuffer(registryInfo));

            String name = registryInfo.getAdapterString();

            if (name == null) {
                name = "<unknown>";
            }

            return name;
        }
    }

    private static int queryAdapterType(int adapter) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var info = stack.callocInt(1);
            d3dkmtQueryAdapterInfo(adapter, KMTQAITYPE_ADAPTERTYPE, memByteBuffer(info));

            return info.get(0);
        }
    }

    private static PciInfo queryPciInfo(int adapter, int physicalAdapterIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var queryDeviceIds = D3DKMTQueryDeviceIdsStruct.calloc(stack);
            queryDeviceIds.setPhysicalAdapterIndex(0);
            d3dkmtQueryAdapterInfo(adapter, KMTQAITYPE_PHYSICALADAPTERDEVICEIDS, memByteBuffer(queryDeviceIds));

            var deviceIds = queryDeviceIds.getDeviceIds();


            return new PciInfo(deviceIds.getVendorId(),
                    deviceIds.getDeviceId(),
                    (deviceIds.getSubSystemId()<<16)|deviceIds.getSubVendorId());
        }
    }

    private static void d3dkmtQueryAdapterInfo(int adapter, int type, ByteBuffer holder) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var info = D3DKMTQueryAdapterInfoStruct.malloc(stack);
            info.setAdapterHandle(adapter);
            info.setType(type);
            info.setDataPointer(memAddress(holder));
            info.setDataLength(holder.remaining());

            apiCheckError("D3DKMTQueryAdapterInfo", nd3dKmtQueryAdapterInfo(info.address()));
        }
    }

    public static void d3dkmtCacheHybridQueryValue(long luid, int state, boolean userPreferenceQuery, int queryType) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var info = D3DKMTHybridListStruct.malloc(stack);
            info.setAdapterLuid(luid);
            info.setState(state);
            info.setbUserPreferenceQuery(userPreferenceQuery);
            info.setQueryType(queryType);

            apiCheckError("D3DKMTCacheHybridQueryValue", nD3DKMTCacheHybridQueryValue(info.address()));
        }
    }

    private static void d3dkmtSetProperties(int type, ByteBuffer payload) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var properties = D3DKMTProperties.calloc(stack);
            properties.setType(type);
            properties.setSize(payload.remaining());
            properties.setPointer(memAddress(payload));

            apiCheckError("D3DKMTSetProperties", nD3DKMTSetProperties(properties.address()));
        }
    }

    public static void setPciProperties(int type, PciInfo info) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pci = D3DKMTPciStruct.calloc(stack);
            pci.setDevice(info.device);
            pci.setVendor(info.vendor);
            pci.setSubSys(info.subsys);
            d3dkmtSetProperties(type, MemoryUtil.memByteBuffer(pci.address(), pci.sizeof()));//Cannot figure out how to do this nicely
        }
    }

    private static int d3dkmtCloseAdapter(int handle) {
        try (var stack = MemoryStack.stackPush()) {
            var info = stack.ints(handle);
            return nD3DKMTCloseAdapter(memAddress(info));
        }
    }

    private static void apiCheckError(String name, int error) {
        if (error != 0) {
            throw new RuntimeException("%s returned non-zero result (error=%s)".formatted(name, Integer.toHexString(error)));
        }
    }

    public record PciInfo(
            int vendor,
            int device,
            int subsys) {
        @Override
        public String toString() {
            return String.format("PciInfo{%X&%X&%X}", this.vendor, this.device, this.subsys);
        }
    }


    public record WDDMAdapterInfo(
            @NotNull GraphicsAdapterVendor vendor,
            @NotNull String name,
            int adapterType,
            long luid,
            String openglIcdFilePath,
            WindowsFileVersion openglIcdVersion,
            PciInfo pciInfo
    ) implements GraphicsAdapterInfo {
        public String getOpenGlIcdName() {
            return D3DKMT.getOpenGlIcdName(this.name);
        }

        @Override
        public String toString() {
            return "AdapterInfo{vendor=%s, description='%s', adapterType=0x%08X, openglIcdFilePath='%s', openglIcdVersion=%s, pciInfo=%s}"
                    .formatted(this.vendor, this.name, this.adapterType, this.openglIcdFilePath, this.openglIcdVersion, this.pciInfo);
        }
    }

    private static String getOpenGlIcdName(String path) {
        return FilenameUtils.removeExtension(FilenameUtils.getName(path));
    }
}
