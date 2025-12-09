 import org.gradle.api.Project

object BuildConfig {
    val MINECRAFT_VERSION: String = "1.21.11-rc3"
    val NEOFORGE_VERSION: String = "21.11.0-alpha.1.21.11-pre3.20251201.145838"
    val FABRIC_LOADER_VERSION: String = "0.18.1"
    val FABRIC_API_VERSION: String = "0.139.4+1.21.11"
    val SUPPORT_FRAPI : Boolean = false

    // This value can be set to null to disable Parchment.
    val PARCHMENT_VERSION: String? = null

    // https://semver.org/
    var MOD_VERSION: String = "0.8.0-beta.3"

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

        builder.append("+mc").append(MINECRAFT_VERSION)

        if (!isReleaseBuild) {
            if (buildId != null) {
                builder.append("-build.${buildId}")
            } else {
                builder.append("-local")
            }
        }

        return builder.toString()
    }
}
