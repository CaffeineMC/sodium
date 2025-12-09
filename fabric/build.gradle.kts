import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("multiloader-platform")

    id("fabric-loom") version ("1.13.4")
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

val configurationFrapiModJava: Configuration = configurations.create("frapiJava") {
    isCanBeResolved = true
}

val configurationCommonModResources: Configuration = configurations.create("commonResources") {
    isCanBeResolved = true
}

val configurationFrapiModResources: Configuration = configurations.create("frapiResources") {
    isCanBeResolved = true
}

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationApiModJava(project(path = ":common", configuration = "commonApiJava"))
    configurationCommonModJava(project(path = ":common", configuration = "commonBootJava"))
    if (BuildConfig.SUPPORT_FRAPI) configurationFrapiModJava(project(path = ":frapi", configuration = "frapiMainJava"))

    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonApiResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonBootResources"))
    if (BuildConfig.SUPPORT_FRAPI) configurationFrapiModResources(project(path = ":frapi", configuration = "frapiMainResources"))
}

sourceSets.apply {
    main {
        compileClasspath += configurationCommonModJava
        compileClasspath += configurationApiModJava
        runtimeClasspath += configurationCommonModJava
        runtimeClasspath += configurationApiModJava
        if (BuildConfig.SUPPORT_FRAPI) {
            runtimeClasspath += configurationFrapiModJava
        }
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
    addEmbeddedFabricModule("fabric-rendering-v1")

    if (BuildConfig.SUPPORT_FRAPI) {
        addEmbeddedFabricModule("fabric-renderer-api-v1")
    }

    addEmbeddedFabricModule("fabric-lifecycle-events-v1")
    addEmbeddedFabricModule("fabric-rendering-fluids-v1")
    addEmbeddedFabricModule("fabric-resource-loader-v0")
    addEmbeddedFabricModule("fabric-resource-loader-v1")
    addEmbeddedFabricModule("fabric-transitive-access-wideners-v1")
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
        if (BuildConfig.SUPPORT_FRAPI) {
            from(configurationFrapiModJava)
        }
    }

    val apiJar = register<org.gradle.jvm.tasks.Jar>("apiJar") {
        archiveClassifier.set("api-dev")
        from(configurationApiModJava)
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

    remapJar {
        destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("mods"))
    }

    processResources {
        from(configurationCommonModResources)
        if (BuildConfig.SUPPORT_FRAPI) {
            from(configurationFrapiModResources)
        }
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

            pom.packaging = "jar"
        }
    }
}