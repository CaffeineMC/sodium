package net.caffeinemc.mods.sodium.client.compatibility.workarounds;

import net.caffeinemc.mods.sodium.client.compatibility.environment.OsUtils;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.amd.AmdWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.intel.IntelWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Workarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Workarounds");

    private static final AtomicReference<Set<Reference>> ACTIVE_WORKAROUNDS = new AtomicReference<>(EnumSet.noneOf(Reference.class));

    public static void init() {
        var workarounds = findNecessaryWorkarounds();

        if (!workarounds.isEmpty()) {
            LOGGER.warn("Sodium has applied one or more workarounds to prevent crashes or other issues on your system: [{}]",
                    workarounds.stream()
                            .map(Enum::name)
                            .collect(Collectors.joining(", ")));
            LOGGER.warn("This is not necessarily an issue, but it may result in certain features or optimizations being " +
                    "disabled. You can sometimes fix these issues by upgrading your graphics driver.");
        }

        ACTIVE_WORKAROUNDS.set(workarounds);
    }

    private static Set<Reference> findNecessaryWorkarounds() {
        var workarounds = EnumSet.noneOf(Reference.class);
        var operatingSystem = OsUtils.getOs();

        if (NvidiaWorkarounds.isNvidiaGraphicsCardPresent()) {
            workarounds.add(Reference.NVIDIA_THREADED_OPTIMIZATIONS_BROKEN);
        }

        if (AmdWorkarounds.isAmdGraphicsCardPresent()) {
            workarounds.add(Reference.AMD_GAME_OPTIMIZATION_BROKEN);
        }

        if (IntelWorkarounds.isUsingIntelGen8OrOlder()) {
            workarounds.add(Reference.INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED);
            workarounds.add(Reference.INTEL_DEPTH_BUFFER_COMPARISON_UNRELIABLE);
        }

        if (operatingSystem == OsUtils.OperatingSystem.LINUX) {
            workarounds.add(Reference.NO_ERROR_CONTEXT_UNSUPPORTED);
        }

        return Collections.unmodifiableSet(workarounds);
    }

    public static boolean isWorkaroundEnabled(Reference id) {
        return ACTIVE_WORKAROUNDS.get()
                .contains(id);
    }

    public enum Reference {
        /**
         * The NVIDIA driver applies "Threaded Optimizations" when Minecraft is detected, causing severe
         * performance issues and crashes.
         * <a href="https://github.com/CaffeineMC/sodium/issues/1816">GitHub Issue</a>
         */
        NVIDIA_THREADED_OPTIMIZATIONS_BROKEN,

        /**
         * Requesting a No Error Context causes a crash at startup when using a Wayland session on GLFW
           <3.4.
         * <a href="https://github.com/CaffeineMC/sodium/issues/1624">GitHub Issue</a>
         */
        NO_ERROR_CONTEXT_UNSUPPORTED,

        /**
         * Intel's graphics driver for Gen8 and older seems to be faulty and causes a crash when calling
         * glFramebufferBlit after the window loses focus.
         * <a href="https://github.com/CaffeineMC/sodium/issues/2727">GitHub Issue</a>
         * <a href="https://github.com/CaffeineMC/sodium/issues/3226">GitHub Issue</a>
         */
        INTEL_FRAMEBUFFER_BLIT_CRASH_WHEN_UNFOCUSED,

        /**
         * Intel's graphics driver for Gen8 and older does not respect depth comparison rules per the OpenGL
         * specification, causing block model overlays to Z-fight when the overlay is on a different render pass than
         * the base model.
         * <a href="https://github.com/CaffeineMC/sodium/issues/2830">GitHub Issue</a>
         */
        INTEL_DEPTH_BUFFER_COMPARISON_UNRELIABLE,

        /**
         * AMD's graphics driver starting at 25.10.2 does not correctly handle glMapBufferRange
         * when minecraft is detected, causing terrain rendering to go invisible if the launcher allows minecraft
         * to be detected. Most commonly this happens with some third party PVP clients,
         * but can happen with other launchers.
         * <a href="https://github.com/CaffeineMC/sodium/issues/3318">GitHub Issue</a>
         */
        AMD_GAME_OPTIMIZATION_BROKEN
    }
}
