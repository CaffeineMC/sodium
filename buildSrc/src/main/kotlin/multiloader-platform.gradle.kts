plugins {
    id("multiloader-base")
    id("maven-publish")
}

val configurationDesktopIntegrationJava: Configuration = configurations.create("commonDesktopIntegration") {
    isCanBeResolved = true
}

dependencies {
    configurationDesktopIntegrationJava(project(path = ":common", configuration = "commonDesktopJava"))
}

tasks {
    processResources {
        inputs.property("version", version)

        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
            expand(mapOf("version" to inputs.properties["version"]))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        from(rootDir.resolve("LICENSE.md"))

        // Entry-point for desktop integration when the file is executed directly
        from(configurationDesktopIntegrationJava)
        manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    }
}

publishing {
    // Each platform is responsible for their own "publications".

    repositories {
        val isReleaseBuild = project.hasProperty("build.release")
        val caffeineMCMavenUsername: String? by project // reads from ORG_GRADLE_caffeineMCMavenUsername
        val caffeineMCMavenPassword: String? by project // reads from ORG_GRADLE_caffeineMCMavenPassword

        if (caffeineMCMavenUsername != null && caffeineMCMavenPassword != null) {
            maven {
                name = "CaffeineMC"
                url = uri("https://maven.caffeinemc.net".let {
                    if (isReleaseBuild) "$it/release" else "$it/snapshot"
                })

                credentials {
                    username = caffeineMCMavenUsername
                    password = caffeineMCMavenPassword
                }
            }
        }
    }
}
