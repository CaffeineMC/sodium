 import org.gradle.api.Project

object BuildConfig {
    val MINECRAFT_VERSION: String = "26.1.2"
    val NEOFORGE_VERSION: String = "26.1.2.10-beta"
    val FABRIC_LOADER_VERSION: String = "0.19.1"
    val FABRIC_API_VERSION: String = "0.145.4+26.1.2"
    val SUPPORT_FRAPI : Boolean = true

    // This value can be set to null to disable Parchment.
    val PARCHMENT_VERSION: String? = null

    // https://semver.org/
    var MOD_VERSION: String = "0.8.9"

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
