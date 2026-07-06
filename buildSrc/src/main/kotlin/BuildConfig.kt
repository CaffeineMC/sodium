import org.gradle.api.Project

object BuildConfig {
    val MINECRAFT_VERSION: String = "1.21.1"
    val NEOFORGE_VERSION: String = "21.1.228"
    val FABRIC_LOADER_VERSION: String = "0.19.2"
    val FABRIC_API_VERSION: String = "0.116.11+1.21.1"

    // This value can be set to null to disable Parchment.
    val PARCHMENT_VERSION: String? = "2024.11.17"

    // https://semver.org/
    val MOD_VERSION: String = "0.8.12"

    val MINECRAFT_VERSION_SHORT: String = MINECRAFT_VERSION
            .replace("-snapshot-", "s")
            .replace("-pre-", "p")
            .replace("-rc-", "r")

    val RELEASE_TAG: String = "mc$MINECRAFT_VERSION_SHORT-$MOD_VERSION"

    val CURSEFORGE_PROJECT_ID = "394468"
    val MODRINTH_PROJECT_ID = "AANobbMI"

    fun createVersionString(project: Project): String {
        val builder = StringBuilder()

        val isReleaseBuild = project.hasProperty("build.release")
        val buildId = System.getenv("GITHUB_RUN_NUMBER")

        if (isReleaseBuild) {
            builder.append(MOD_VERSION)
        } else {
            builder.append(MOD_VERSION.substringBefore('-'))
            builder.append("-SNAPSHOT")
        }

        builder.append("+mc").append(MINECRAFT_VERSION_SHORT)

        if (!isReleaseBuild) {
            if (buildId != null) {
                builder.append("-build.${buildId}")
            } else {
                builder.append("-local")
            }
        }

        return builder.toString()
    }

    fun calculateGitHash(project: Project): String = try {
        val output = project.providers.exec {
            workingDir(project.projectDir)
            commandLine("git", "rev-parse", "HEAD")
        }
        output.standardOutput.asText.get().trim()
    } catch (_: Throwable) {
        "unknown"
    }

    fun getChangelog(project: Project): String = project.rootProject.file("CHANGELOG.md").readText()
            .split("----------")[1]
            .trim()
            .replace("[ReleaseTag]()", RELEASE_TAG)
            .replace("[MCVersion]()", MINECRAFT_VERSION)
            .replace("[SodiumVersion]()", MOD_VERSION)
}
