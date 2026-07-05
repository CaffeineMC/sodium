package net.caffeinemc.mods.sodium.service;

import net.caffeinemc.mods.sodium.client.compatibility.checks.PreLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.amd.AmdWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SodiumWorkarounds implements GraphicsBootstrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-Workarounds");
    private static final long EARLY_WINDOW_RESTORE_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(30);
    private static final long EARLY_WINDOW_RESTORE_POLL_INTERVAL_MILLIS = 10L;

    @Override
    public String name() {
        return "sodium";
    }

    @Override
    public void bootstrap(String[] arguments) {
        PreLaunchChecks.checkEnvironment();
        GraphicsAdapterProbe.findAdapters();
        Workarounds.init();

        // When early window control is disabled, NeoForge creates no early GL context, so context creation happens
        // later in Window#createGlfwWindow. We want to avoid applying the workarounds twice if the context is going to
        // be created later and our mixin to Window#createGlfwWindow runs.
        // See https://github.com/CaffeineMC/sodium/issues/3664 for more details.
        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            NvidiaWorkarounds.applyEnvironmentChanges();
            AmdWorkarounds.applyEnvironmentChanges();
            restoreEnvironmentChangesAfterEarlyWindowInit();
        }
    }

    private static void restoreEnvironmentChangesAfterEarlyWindowInit() {
        Thread thread = new Thread(SodiumWorkarounds::waitForEarlyWindowAndRestoreEnvironmentChanges,
                "sodium-workaround-cleanup");
        thread.setDaemon(true);
        thread.start();
    }

    private static void waitForEarlyWindowAndRestoreEnvironmentChanges() {
        try {
            if (!waitForEarlyWindowRenderer()) {
                LOGGER.warn("Timed out waiting for NeoForge's early renderer; restoring graphics " +
                        "driver workaround environment changes now");
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed while waiting for NeoForge's early renderer; restoring graphics " +
                    "driver workaround environment changes now", t);
        } finally {
            NvidiaWorkarounds.undoEnvironmentChanges();
            AmdWorkarounds.undoEnvironmentChanges();
        }
    }

    private static boolean waitForEarlyWindowRenderer() throws ReflectiveOperationException, InterruptedException {
        Class<?> windowHandlerClass = Class.forName("net.neoforged.fml.loading.ImmediateWindowHandler");
        Field providerField = windowHandlerClass.getDeclaredField("provider");
        providerField.setAccessible(true);

        long deadline = System.nanoTime() + EARLY_WINDOW_RESTORE_TIMEOUT_NANOS;

        while (System.nanoTime() < deadline) {
            Object provider = providerField.get(null);

            if (provider != null && hasEarlyWindowRendererInitialized(provider)) {
                return true;
            }

            Thread.sleep(EARLY_WINDOW_RESTORE_POLL_INTERVAL_MILLIS);
        }

        return false;
    }

    private static boolean hasEarlyWindowRendererInitialized(Object provider) throws ReflectiveOperationException {
        // NeoForge >=21.10.x initializes the early GL context from a renderer future after creating the provider window.
        // Waiting for it avoids restoring the spoofed command line before the driver has initialized.
        Field rendererFutureField = findField(provider.getClass(), "rendererFuture");

        if (rendererFutureField != null) {
            rendererFutureField.setAccessible(true);
            Object rendererFuture = rendererFutureField.get(provider);

            return rendererFuture instanceof Future<?> future && future.isDone();
        }

        Field windowField = findField(provider.getClass(), "window");

        if (windowField != null) {
            windowField.setAccessible(true);
            return windowField.getLong(provider) != 0L;
        }

        return false;
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;

        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}
