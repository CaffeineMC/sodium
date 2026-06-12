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

        val releaseDestination: String = project.providers.gradleProperty("build.release.destination").orNull
                ?: return@publishMods println("build.release.destination must be defined (expected: GH+MR+CF, GH+MR, GH)")
        val publishModrinth = releaseDestination.contains("MR")
        val publishCurseforge = releaseDestination.contains("CF")

        val modVersion = BuildConfig.createVersionString(project);

        type = when {
            modVersion.contains("alpha") -> ReleaseType.ALPHA
            modVersion.contains("beta") -> ReleaseType.BETA
            else -> ReleaseType.STABLE
        }
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

        setupFor("Fabric", releasePlatform, publishCurseforge, publishModrinth, curseforgeShared, modrinthShared)
        setupFor("NeoForge", releasePlatform, publishCurseforge, publishModrinth, curseforgeShared, modrinthShared)

        github {
            accessToken = project.providers.environmentVariable("GITHUB_TOKEN")
            repository = "CaffeineMC/sodium"
            commitish = BuildConfig.calculateGitHash(project)
            version = BuildConfig.RELEASE_TAG
            displayName = "Sodium ${BuildConfig.MOD_VERSION} for Minecraft ${BuildConfig.MINECRAFT_VERSION}"
            file.unset()
            file.unsetConvention()

            allowEmptyFiles = true
        }
    }
}

fun me.modmuss50.mpp.ModPublishExtension.setupFor(loaderName: String, releasePlatform: String, publishCurseforge: Boolean, publishModrinth: Boolean, curseforgeOptions: Provider<CurseforgeOptions>, modrinthOptions: Provider<ModrinthOptions>) {
    val loaderLowercase = loaderName.lowercase(Locale.ROOT)

    if (releasePlatform == "both" || releasePlatform == loaderLowercase) {
        val taskName = if (loaderLowercase == "fabric") "remapJar" else "jar"
        val jar = project(":$loaderLowercase").tasks.getByName(taskName).outputs.files.singleFile

        val releaseTitle = "Sodium ${BuildConfig.MOD_VERSION} for $loaderName ${BuildConfig.MINECRAFT_VERSION}"
        val releaseVersion = "${BuildConfig.RELEASE_TAG}-$loaderLowercase"

        if (publishCurseforge) {
            curseforge("curseforge$loaderName") {
                from(curseforgeOptions)

                file.set(jar)
                displayName = releaseTitle
                version = releaseVersion
                modLoaders.add(loaderLowercase)

                clientRequired = true
                serverRequired = false
            }
        }

        if (publishModrinth) {
            modrinth("modrinth$loaderName") {
                from(modrinthOptions)

                file.set(jar)
                displayName = releaseTitle
                version = releaseVersion
                modLoaders.add(loaderLowercase)
            }
        }
    }
}