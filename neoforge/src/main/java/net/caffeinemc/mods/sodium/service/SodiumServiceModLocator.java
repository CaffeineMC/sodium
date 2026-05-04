package net.caffeinemc.mods.sodium.service;

import cpw.mods.jarhandling.JarContents;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Mounts the JiJ entries packaged inside the sodium service jar and hands them to FML's mod
 * discovery pipeline. Refer to <a href="https://github.com/neoforged/FancyModLoader/commit/c984deffb4115dd0e4d5a9958a33b5b45adfcade">FML #375</a>.
 */
public class SodiumServiceModLocator implements IModFileCandidateLocator {
    private static final String JIJ_DIR = "META-INF/jarjar";

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        Path jijDir = locateServiceRoot().resolve(JIJ_DIR);
        try (Stream<Path> entries = Files.list(jijDir)) {
            entries.filter(SodiumServiceModLocator::isJar).forEach(entry -> mountAndAdd(entry, pipeline));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list JiJ entries in " + jijDir, e);
        }
    }

    @SuppressWarnings("resource") // innerFs ownership transfers to JarContents
    private static void mountAndAdd(Path innerJarInsideService, IDiscoveryPipeline pipeline) {
        try {
            String specific = innerJarInsideService.toAbsolutePath().toUri().getRawSchemeSpecificPart();
            URI jijUri = new URI("jij:" + specific).normalize();
            FileSystem innerFs = FileSystems.newFileSystem(jijUri, Map.of("packagePath", innerJarInsideService));
            JarContents contents = JarContents.of(innerFs.getPath("/"));
            pipeline.addJarContent(contents, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException("Failed to add JiJ entry " + innerJarInsideService.getFileName() + " to mod discovery", e);
        }
    }

    private static boolean isJar(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".jar");
    }

    private static Path locateServiceRoot() {
        var cs = SodiumServiceModLocator.class.getProtectionDomain().getCodeSource();
        if (cs == null || cs.getLocation() == null) {
            throw new IllegalStateException("CodeSource unavailable; cannot resolve sodium service jar.");
        }
        URI csUri;
        try {
            csUri = cs.getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not parse CodeSource location URI " + cs.getLocation(), e);
        }
        try {
            return Paths.get(csUri);
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve sodium service jar from CodeSource URI " + csUri, e);
        }
    }
}
