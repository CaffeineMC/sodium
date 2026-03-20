package net.caffeinemc.mods.sodium.client.compatibility.workarounds.amd;

import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterVendor;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.caffeinemc.mods.sodium.client.platform.windows.WindowsCommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmdWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-AmdWorkarounds");

    public static boolean isAmdGraphicsCardPresent() {
        return GraphicsAdapterProbe.getAdapters()
                .stream()
                .anyMatch(adapter -> adapter.vendor() == GraphicsAdapterVendor.AMD);
    }

    public static void undoEnvironmentChanges() {
        if (OsUtils.getOs() == OsUtils.OperatingSystem.WIN) {
            undoEnvironmentChanges$Windows();
        }
    }

    private static void undoEnvironmentChanges$Windows() {
        WindowsCommandLine.resetCommandLine();
    }

    public static void applyEnvironmentChanges() {
        // We can't know if the OpenGL context will actually be initialized using the AMD ICD, but we need to
        // modify the process environment *now* otherwise the driver will initialize with bad settings. For non-AMD
        // drivers, these workarounds are not likely to cause issues.
        if (!isAmdGraphicsCardPresent()) {
            return;
        }

        // Skip applying the AMD workaround if the user also has an Nvidia gpu, to avoid attempting to overwrite the
        // process command line twice.
        if (NvidiaWorkarounds.isNvidiaGraphicsCardPresent()) {
            return;
        }

        LOGGER.info("Modifying process environment to apply workarounds for the AMD graphics driver...");

        try {
            if (OsUtils.getOs() == OsUtils.OperatingSystem.WIN) {
                applyEnvironmentChanges$Windows();
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to modify the process environment", t);
            logWarning();
        }
    }


    private static void applyEnvironmentChanges$Windows() {
        // The new AMD drivers rely on parsing the command line arguments to detect Minecraft.
        // When they do they apply an optimization that is broken with sodium present
        // This stops AMD drivers from detecting the game
        WindowsCommandLine.setCommandLine("net.caffeinemc.sodium / net.minecraft.client.main.Main /");
    }

    private static void logWarning() {
        LOGGER.error("READ ME!");
        LOGGER.error("READ ME! The workarounds for the AMD Graphics Driver did not apply correctly!");
        LOGGER.error("READ ME! You may run into unexplained graphical issues.");
        LOGGER.error("READ ME! More information about what went wrong can be found above this message.");
        LOGGER.error("READ ME!");
        LOGGER.error("READ ME! Please help us understand why this problem occurred by opening a bug report on our issue tracker:");
        LOGGER.error("READ ME!   https://github.com/CaffeineMC/sodium/issues");
        LOGGER.error("READ ME!");
    }
}
