plugins {
    id("multiloader-platform")

    id("net.neoforged.moddev") version("2.0.107")
}

base {
    archivesName = "sodium-neoforge"
}

repositories {
    maven("https://maven.irisshaders.dev/releases")

    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")
}

sourceSets {
    create("service")
}

val configurationCommonModJava: Configuration = configurations.create("commonModJava") {
    isCanBeResolved = true
}
val configurationCommonModResources: Configuration = configurations.create("commonModResources") {
    isCanBeResolved = true
}

val configurationCommonServiceJava: Configuration = configurations.create("commonServiceJava") {
    isCanBeResolved = true
}
val configurationCommonServiceResources: Configuration = configurations.create("commonServiceResources") {
    isCanBeResolved = true
}

dependencies {
    configurationCommonModJava(project(path = ":common", configuration = "commonMainJava"))
    configurationCommonModJava(project(path = ":common", configuration = "commonApiJava"))
    configurationCommonServiceJava(project(path = ":common", configuration = "commonBootJava"))

    configurationCommonModResources(project(path = ":common", configuration = "commonMainResources"))
    configurationCommonModResources(project(path = ":common", configuration = "commonApiResources"))
    configurationCommonServiceResources(project(path = ":common", configuration = "commonBootResources"))

    fun addEmbeddedFabricModule(dependency: String) {
        dependencies.implementation(dependency)
        dependencies.jarJar(dependency)
    }

    //addEmbeddedFabricModule("org.sinytra.forgified-fabric-api:fabric-block-view-api-v2:1.0.10+9afaaf8c19")

    jarJar(project(":neoforge", "mod"))
}

val modJar = tasks.register<Jar>("modJar") {
    from(configurationCommonModJava)
    from(configurationCommonModResources)

    from(sourceSets["mod"].output)

    from(rootDir.resolve("LICENSE.md"))

    filesMatching(listOf("META-INF/neoforge.mods.toml")) {
        expand(mapOf("version" to inputs.properties["version"]))
    }

    archiveClassifier = "mod"
}

val configurationMod: Configuration = configurations.create("mod") {
    isCanBeConsumed = true
    isCanBeResolved = true

    outgoing {
        artifact(modJar)
    }
}

sourceSets {
    named("main") {
        compileClasspath += configurationCommonServiceJava
        runtimeClasspath += configurationCommonServiceJava
    }

    create("mod") {
        compileClasspath = sourceSets["main"].compileClasspath
        runtimeClasspath = sourceSets["main"].runtimeClasspath

        compileClasspath += configurationCommonModJava
        runtimeClasspath += configurationCommonModJava
    }
}

neoForge {
    version = BuildConfig.NEOFORGE_VERSION

    if (BuildConfig.PARCHMENT_VERSION != null) {
        parchment {
            minecraftVersion = BuildConfig.MINECRAFT_VERSION
            mappingsVersion = BuildConfig.PARCHMENT_VERSION
        }
    }

    runs {
        create("Client") {
            client()
            ideName = "NeoForge/Client"
        }
    }

    mods {
        create("sodium") {
            sourceSet(sourceSets["mod"])
            sourceSet(project(":common").sourceSets["main"])
            sourceSet(project(":common").sourceSets["api"])
        }

        create("sodium-service") {
            sourceSet(sourceSets["main"])
            sourceSet(project(":common").sourceSets["boot"])
        }
    }
}

tasks {
    jar {
        from(configurationCommonServiceJava)
        manifest.attributes["FMLModType"] = "LIBRARY"
        manifest.attributes["Automatic-Module-Name"] = "sodium_service"

        destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("mods"))

        from(sourceSets.getByName("mod").output.resourcesDir!!.resolve("META-INF/neoforge.mods.toml")) {
            into("META-INF")
        }

        from(project(":common").sourceSets.main.get().output.resourcesDir!!.resolve("sodium-icon.png"))
    }

    processResources {
        from(configurationCommonServiceResources)
    }

    getByName<ProcessResources>("processModResources") {
        eachFile {
            println(path)
        }
        filesMatching(listOf("META-INF/neoforge.mods.toml")) {
            expand(mapOf("version" to BuildConfig.createVersionString(rootProject)))
        }
    }
}

