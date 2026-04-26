import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.curseforge.CurseforgeOptions
import me.modmuss50.mpp.platforms.modrinth.ModrinthOptions
import java.util.*

plugins {
    id("me.modmuss50.mod-publish-plugin") version("1.1.0")
}

gradle.projectsEvaluated {
    publishMods {
        if (!project.hasProperty("build.release")) {
            return@publishMods println("Publishing is disabled, please use the CI publishing workflow")
        }

        val releasePlatform: String = project.providers.gradleProperty("build.release.platform").orNull
                ?: return@publishMods println("build.release.platform must be defined (expected: both, fabric, neoforge)")

        val modVersion = BuildConfig.createVersionString(project);

        type = when {
            modVersion.contains("alpha") -> ReleaseType.ALPHA
            modVersion.contains("beta") -> ReleaseType.BETA
            else -> ReleaseType.STABLE
        }
        version = modVersion
        changelog = BuildConfig.getChangelog(project)

        val curseforgeShared = curseforgeOptions {
            accessToken = project.providers.environmentVariable("CURSEFORGE_API_KEY")
            projectId = BuildConfig.CURSEFORGE_PROJECT_ID
            minecraftVersions.add(BuildConfig.MINECRAFT_VERSION)
        }

        val modrinthShared = modrinthOptions {
            accessToken = project.providers.environmentVariable("MODRINTH_API_KEY")
            projectId = BuildConfig.MODRINTH_PROJECT_ID
            minecraftVersions.add(BuildConfig.MINECRAFT_VERSION)
        }

        setupFor("Fabric", releasePlatform, curseforgeShared, modrinthShared)
        setupFor("NeoForge", releasePlatform, curseforgeShared, modrinthShared)

        github {
            accessToken = project.providers.environmentVariable("GITHUB_TOKEN")
            repository = "CaffeineMC/sodium"
            commitish = BuildConfig.calculateGitHash(project)
            tagName = BuildConfig.RELEASE_TAG
            displayName = "Sodium ${BuildConfig.MOD_VERSION} for Minecraft ${BuildConfig.MINECRAFT_VERSION}"
            file.unset()
            file.unsetConvention()

            allowEmptyFiles = true
        }
    }
}

fun me.modmuss50.mpp.ModPublishExtension.setupFor(loaderName: String, releasePlatform: String, curseforgeOptions: Provider<CurseforgeOptions>, modrinthOptions: Provider<ModrinthOptions>) {
    val loaderLowercase = loaderName.lowercase(Locale.ROOT)

    if (releasePlatform == "both" || releasePlatform == loaderLowercase) {
        val jar = project(":$loaderLowercase").tasks.named<Jar>("jar").get().archiveFile

        curseforge("curseforge$loaderName") {
            from(curseforgeOptions)
            
            file.set(jar)
            displayName = "Sodium ${BuildConfig.MOD_VERSION} for $loaderName"
            modLoaders.add(loaderLowercase)

            clientRequired = true
            serverRequired = false
        }

        modrinth("modrinth$loaderName") {
            from(modrinthOptions)

            file.set(jar)
            displayName = "Sodium ${BuildConfig.MOD_VERSION} for $loaderName on ${BuildConfig.MINECRAFT_VERSION}"
            version = "${BuildConfig.MOD_VERSION}+mc${BuildConfig.MINECRAFT_VERSION}-$loaderLowercase"
            modLoaders.add(loaderLowercase)
        }
    }
}