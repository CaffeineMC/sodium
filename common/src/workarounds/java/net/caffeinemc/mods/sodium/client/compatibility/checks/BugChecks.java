package net.caffeinemc.mods.sodium.client.compatibility.checks;

/**
 * "Checks" are used to determine whether the environment we are running within is actually reasonable. Most often,
 * failing checks will crash the game and prompt the user for intervention.
 */
class BugChecks {
    /**
     * Some older drivers for Intel Gen7 Graphics on Windows are defective and will never return from
     * a call to <pre>glClientWaitSync</pre>. As there is no way to recover the main thread after the
     * deadlock occurs, trying to work around this seems to be impossible. Updating the driver to version
     * 15.33.53.5161 resolves the problem, and is the currently recommended solution.
     * <a href="https://github.com/CaffeineMC/sodium/issues/899">GitHub Issue</a>
     */
    public static final boolean ISSUE_899 = configureCheck("issue899", true);

    /**
     * Since version 526.47 of the NVIDIA Graphics Driver, the OpenGL user-mode driver will attempt to detect
     * Minecraft with hard-coded logic in the driver, and enable broken optimizations (referred to as "Threaded
     * Optimizations"). This will cause crashes during framebuffer initialization (and some draw calls) for unclear
     * reasons, especially on systems with hybrid graphics. Furthermore, performance is often severely degraded
     * due to bubbles in the NVIDIA command submission thread, especially when other mods query context state.
     * <p>
     * Sodium can prevent the detection of Minecraft and the enablement of these unstable optimizations, but the logic
     * is extensive and depends heavily on the exact operating system and graphics driver version used. Some older
     * graphics driver versions have no workaround available presently.
     * <a href="https://github.com/CaffeineMC/sodium/issues/1486">GitHub Issue</a>
     */
    public static final boolean ISSUE_1486 = configureCheck("issue1486", true);

    /**
     * Older versions of the RivaTuner Statistics Server will attempt to use legacy OpenGL functions in a Core
     * profile, which causes either a crash or excessive log spam, depending on the OpenGL implementation. This is
     * because RivaTuner relies on replacing the OpenGL context immediately after the application initializes it,
     * but does so improperly when a No Error Context is used. This problem can't be avoided by simply configuring
     * RivaTuner to not inject into Minecraft, as it *always* injects first to modify the context, and *then* disables
     * itself.
     * <a href="https://github.com/CaffeineMC/sodium/issues/2048">GitHub Issue</a>
     */
    public static final boolean ISSUE_2048 = configureCheck("issue2048", true);

    /**
     * LWJGL does not provide API stability guarantees for other libraries using it to create their own C bindings.
     * Because of this, the game will crash due to breaking method/type signature changes very early at startup. We
     * should not be using these "internal" classes in LWJGL, but the amount of work involved doing everything ourselves
     * is astronomical. When Minecraft ships a version of OpenJDK with the Foreign Function & Memory API, we can replace
     * our dependency on LWJGL and remove this check.
     * <a href="https://github.com/CaffeineMC/sodium/issues/2561">GitHub Issue</a>
     */
    public static final boolean ISSUE_2561 = configureCheck("issue2561", true);

    /**
     * ASUS's GPU Tweak III does not correctly restore OpenGL context state after rendering its in-game overlay,
     * which frequently causes graphical corruption, excessive log file spam, and crashes. These problems are
     * actually reproducible without any mods installed, but it seems to be exacerbated with Sodium due to how different
     * the rendering pipeline is. This problem can be avoided by configuring the application to not inject into
     * Minecraft (or Java applications).
     * <a href="https://github.com/CaffeineMC/sodium/issues/2637">GitHub Issue</a>
     */
    public static final boolean ISSUE_2637 = configureCheck("issue2637", true);

    private static boolean configureCheck(String name, boolean defaultValue) {
        var propertyValue = System.getProperty(getPropertyKey(name), null);

        if (propertyValue == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(propertyValue);
    }

    private static String getPropertyKey(String name) {
        return "sodium.checks." + name;
    }
}
