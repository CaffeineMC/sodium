plugins {
    id("multiloader-platform")

    id("net.neoforged.moddev") version("2.0.122")
}

base {
    archivesName = "sodium-neoforge"
}

repositories {
    maven("https://maven.irisshaders.dev/releases")

    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.neoforged.net/releases/")
    maven {
        name = "Maven for PR #2815" // https://github.com/neoforged/NeoForge/pull/2815
        url = uri("https://prmaven.neoforged.net/NeoForge/pr2815")
        content {
            includeModule("net.neoforged", "neoforge")
            includeModule("net.neoforged", "testframework")
        }
    }
}

sourceSets {
    create("service")
}

val configurationCommonModJava: Configuration = configurations.create("commonModJava") {
    isCanBeResolved = true
}
val configurationCommonApiJava: Configuration = configurations.create("commonApiJava") {
    isCanBeResolved = true
}
val configurationCommonApiSources: Configuration = configurations.create("apiSources") {
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
    configurationCommonApiJava(project(path = ":common", configuration = "commonApiJava"))
    configurationCommonServiceJava(project(path = ":common", configuration = "commonBootJava"))

    configurationCommonApiSources(project(path = ":common", configuration = "commonApiSources"))

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
    from(configurationCommonApiJava)
    from(configurationCommonModResources)

    from(sourceSets["mod"].output)

    from(rootDir.resolve("LICENSE.md"))

    filesMatching(listOf("META-INF/neoforge.mods.toml")) {
        expand(mapOf("version" to inputs.properties["version"]))
    }

    archiveClassifier = "mod"
}

val apiJar = tasks.register<Jar>("apiJar") {
    from(configurationCommonApiJava)

    from(rootDir.resolve("LICENSE.md"))

    archiveClassifier = "api"

    destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("api"))
}

val apiSourcesJar = tasks.register<Jar>("apiSourcesJar") {
    from(configurationCommonApiSources)

    from(rootDir.resolve("LICENSE.md"))

    archiveClassifier = "api-sources"

    destinationDirectory.set(file(rootProject.layout.buildDirectory).resolve("api-sources"))
}

tasks.jar {
    dependsOn(apiJar)
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
        compileClasspath += configurationCommonApiJava
        runtimeClasspath += configurationCommonModJava
        runtimeClasspath += configurationCommonApiJava
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

            artifact(apiJar) {
                classifier = null
            }

            artifact(apiSourcesJar) {
                classifier = "sources"
            }

            pom.packaging = "jar"
        }

        create<MavenPublication>("mavenMod") {
            groupId = project.group as String
            artifactId = rootProject.name + "-" + project.name + "-mod"
            version = version

            artifact(modJar) {
                classifier = null
            }

            pom.packaging = "jar"
        }
    }
}

