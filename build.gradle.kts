plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.cocoapods) apply false
}

tasks.register("setBuildVersion") {
    // Categorizes this task under 'versioning' in Gradle task list
    group = "versioning"
    description = "Updates the version in gradle.properties"

    val pNewVersion = project.providers.gradleProperty("newVersion").orElse("")

    doLast {
        val newVersion = pNewVersion.get()
        if (newVersion.isBlank()) {
            logger.warn("Warning: -PnewVersion not provided.")
            return@doLast
        }

        val propertiesFile = layout.projectDirectory.file("gradle.properties").asFile
        if (propertiesFile.exists()) {
            val lines = propertiesFile.readLines()
            val newLines = lines.map { line ->
                if (line.trim().startsWith("version=")) {
                    "version=$newVersion"
                } else {
                    line
                }
            }
            propertiesFile.writeText(newLines.joinToString("\n"))
            logger.lifecycle("Updated gradle.properties to version: $newVersion")
        }
    }
}
