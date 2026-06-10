package net.caffeinemc.mods.sodium.service;

import net.caffeinemc.mods.sodium.client.compatibility.checks.PreLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.amd.AmdWorkarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;

public class SodiumWorkarounds implements GraphicsBootstrapper {
    @Override
    public String name() {
        return "sodium";
    }

    @Override
    public void bootstrap(String[] arguments) {
        PreLaunchChecks.checkEnvironment();
        GraphicsAdapterProbe.findAdapters();
        Workarounds.init();

        // When early window control is disabled, NeoForge creates no early GL context, so context creation happens later in Window#createGlfwWindow. We want to avoid doing the workarounds twice if the context is going to be created later and thus our mixin to Window#createGlfwWindow runs that also applies them.
        // See https://github.com/CaffeineMC/sodium/issues/3664 for more details.
        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            NvidiaWorkarounds.applyEnvironmentChanges();
            AmdWorkarounds.applyEnvironmentChanges();
        }
    }
}
