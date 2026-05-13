import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("multiloader-platform")

    id("net.fabricmc.fabric-loom-remap") version ("1.16.1")
}

base {
    archivesName = "sodium-fabric"
}

val configurationApiModJava: Configuration = configurations.create("apiJava") {
    isCanBeResolved = true
}

val configurationCommonModJava: Configuration = configurations.create("commonJava") {
    isCanBeResolved = true
}

val configurationApiModSources: Configuration = configurations.create("apiSources") {
    isCanBeResolved = true
}

val configurationCommonModResources: Configuration = configurations.create("commonResources") {
    isCanBeResolved = true
}

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationApiModJava(project(path = ":common", configuration = "commonApiJava"))
    configurationCommonModJava(project(path = ":common", configuration = "commonBootJava"))

    configurationApiModSources(project(path = ":common", configuration = "commonApiSources"))

    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonApiResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonBootResources"))
}

sourceSets.apply {
    main {
        compileClasspath += configurationCommonModJava
        compileClasspath += configurationApiModJava
        runtimeClasspath += configurationCommonModJava
        runtimeClasspath += configurationApiModJava
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    mappings(loom.layered {
        officialMojangMappings()

        if (BuildConfig.PARCHMENT_VERSION != null) {
            parchment("org.parchmentmc.data:parchment-${BuildConfig.MINECRAFT_VERSION}:${BuildConfig.PARCHMENT_VERSION}@zip")
        }
    })

    modImplementation("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    fun addEmbeddedFabricModule(name: String) {
        val module = fabricApi.module(name, BuildConfig.FABRIC_API_VERSION)
        modImplementation(module)
        include(module)
    }

    // Fabric API modules
    addEmbeddedFabricModule("fabric-api-base")
    addEmbeddedFabricModule("fabric-block-view-api-v2")
    addEmbeddedFabricModule("fabric-renderer-api-v1")
    addEmbeddedFabricModule("fabric-rendering-data-attachment-v1")
    addEmbeddedFabricModule("fabric-rendering-fluids-v1")
    addEmbeddedFabricModule("fabric-resource-loader-v0")
}

loom {
    accessWidenerPath.set(file("src/main/resources/sodium-fabric.accesswidener"))

    mixin {
        useLegacyMixinAp = false
    }

    runs {
        named("client") {
            client()
            configName = "Fabric/Client"
            appendProjectPathToConfigName = false
            ideConfigGenerated(true)
            runDir("run")
        }
    }
}

tasks {
    jar {
        from(configurationCommonModJava)
        from(configurationApiModJava)
    }

    val apiJar = register<org.gradle.jvm.tasks.Jar>("apiJar") {
        archiveClassifier.set("api-dev")
        from(configurationApiModJava)
        from(sourceSets.main.get().resources)
        destinationDirectory.set(file(project.layout.buildDirectory).resolve("devlibs"))
    }

    val apiSourcesJar = register<org.gradle.jvm.tasks.Jar>("apiSourcesJar") {
        archiveClassifier.set("api-sources-dev")
        from(configurationApiModSources)
        from(sourceSets.main.get().resources)
        destinationDirectory.set(file(project.layout.buildDirectory).resolve("devlibs"))
    }

    register<RemapJarTask>("remapApiJar") {
        dependsOn("apiJar")
        archiveClassifier.set("api")
        nestedJars.unset()
        destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("api"))

        inputFile.set(apiJar.flatMap { it.archiveFile })
    }

    register<RemapSourcesJarTask>("remapApiSourcesJar") {
        dependsOn("apiSourcesJar")
        archiveClassifier.set("api-sources")
        destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("api-sources"))

        inputFile.set(apiSourcesJar.flatMap { it.archiveFile })
    }

    remapJar {
        destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("mods"))
    }

    processResources {
        from(configurationCommonModResources)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group as String
            artifactId = rootProject.name + "-" + project.name
            version = version

            from(components["java"])
        }

        create<MavenPublication>("mavenApi") {
            groupId = project.group as String
            artifactId = rootProject.name + "-" + project.name + "-api"
            version = version

            artifact(tasks.named("remapApiJar")) {
                classifier = null
            }

            artifact(tasks.named("remapApiSourcesJar")) {
                classifier = "sources"
            }

            pom.packaging = "jar"
        }
    }
}