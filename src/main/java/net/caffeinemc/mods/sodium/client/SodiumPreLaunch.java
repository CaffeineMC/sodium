package net.caffeinemc.mods.sodium.client;

import net.caffeinemc.mods.sodium.client.compatibility.checks.PreLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class SodiumPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        GraphicsAdapterProbe.findAdapters();
        PreLaunchChecks.onGameInit();
        Workarounds.init();
    }
}
