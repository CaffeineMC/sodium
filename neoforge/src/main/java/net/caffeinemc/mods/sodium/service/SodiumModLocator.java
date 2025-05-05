package net.caffeinemc.mods.sodium.service;

import cpw.mods.jarhandling.JarContents;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class SodiumModLocator implements IModFileCandidateLocator {
    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        try (Stream<Path> stream = Files.walk(Path.of(SodiumModLocator.class.getResource("/META-INF/jarjar/").toURI()))) {
            stream.filter(t -> {
                return !Files.isDirectory(t) && t.getFileName().toString().endsWith(".jar");
            }).forEach(t -> pipeline.addJarContent(JarContents.of(t), ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
