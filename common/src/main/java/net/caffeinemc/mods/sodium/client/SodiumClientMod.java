package net.caffeinemc.mods.sodium.client;

import net.caffeinemc.mods.sodium.client.console.Console;
import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.client.data.fingerprint.FingerprintMeasure;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.SodiumDebugEntry;
import net.caffeinemc.mods.sodium.client.gui.SodiumFpsPercentilesEntry;
import net.caffeinemc.mods.sodium.client.gui.SodiumOptions;
import net.caffeinemc.mods.sodium.client.services.PlatformRuntimeInformation;
import net.minecraft.client.core.registries.ClientRegistries;
import net.minecraft.client.gui.components.debug.DebugEntryNoop;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SodiumClientMod {
    private static SodiumOptions OPTIONS;
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium");
    public static final ResourceKey<DebugScreenEntry> SODIUM_DEBUG_ENTRY_FULL = key(Identifier.fromNamespaceAndPath("sodium", "debug_full"));
    public static final ResourceKey<DebugScreenEntry> SODIUM_DEBUG_ENTRY_REDUCED = key(Identifier.fromNamespaceAndPath("sodium", "debug_reduced"));
    public static final ResourceKey<DebugScreenEntry> SODIUM_DEBUG_ENTRY_BUFFER_ARENA = key(Identifier.fromNamespaceAndPath("sodium", "buffer_arena"));
    public static final ResourceKey<DebugScreenEntry> SODIUM_FPS_PERCENTILES = key(Identifier.fromNamespaceAndPath("sodium", "fps_percentiles"));

    private static ResourceKey<DebugScreenEntry> key(Identifier identifier) {
        return ResourceKey.create(ClientRegistries.DEBUG_SCREEN_ENTRY, identifier);
    }

    private static String MOD_VERSION;

    public static void bootstrapEntries(WritableRegistry<DebugScreenEntry> registry) {
        Registry.register(registry, SODIUM_DEBUG_ENTRY_FULL, new SodiumDebugEntry(true));
        Registry.register(registry, SODIUM_DEBUG_ENTRY_REDUCED, new SodiumDebugEntry(false));
        Registry.register(registry, SODIUM_DEBUG_ENTRY_BUFFER_ARENA, new DebugEntryNoop());
        Registry.register(registry, SODIUM_FPS_PERCENTILES, new SodiumFpsPercentilesEntry());
    }

    public static void onInitialization(String version) {
        MOD_VERSION = version;

        OPTIONS = loadConfig();

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }
    }

    public static SodiumOptions options() {
        if (OPTIONS == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return OPTIONS;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static SodiumOptions loadConfig() {
        try {
            return SodiumOptions.loadFromDisk();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            Console.instance().logMessage(MessageLevel.SEVERE, "sodium.console.config_not_loaded", true, 12.5);

            var config = SodiumOptions.defaults();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        OPTIONS = SodiumOptions.defaults();

        try {
            SodiumOptions.writeToDisk(OPTIONS);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    private static void updateFingerprint() {
        var current = FingerprintMeasure.create();

        if (current == null) {
            return;
        }

        HashedFingerprint saved = null;

        try {
            saved = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            LOGGER.error("Failed to load existing fingerprint",  t);
        }

        if (saved == null || !current.looselyMatches(saved)) {
            HashedFingerprint.writeToDisk(current.hashed());

            OPTIONS.notifications.hasSeenDonationPrompt = false;
            OPTIONS.notifications.hasEditedFullscreenOption = false; // This is to check language again in modpacks...
            OPTIONS.notifications.hasClearedDonationButton = false;

            try {
                SodiumOptions.writeToDisk(OPTIONS);
            } catch (IOException e) {
                LOGGER.error("Failed to update config file", e);
            }
        }
    }

    public static boolean allowDebuggingOptions() {
        return PlatformRuntimeInformation.getInstance().isDevelopmentEnvironment();
    }
}
