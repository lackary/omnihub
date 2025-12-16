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
    group = "versioning"
    description = "Updates the version and build number in gradle.properties and iosApp Config.xcconfig"

    // Receive -PnewVersion (e.g. 1.2.0)
    val pNewVersion = project.providers.gradleProperty("newVersion").orElse("")
    // Receive -PbuildNumber (e.g. 123)
    val pBuildNumber = project.providers.gradleProperty("buildNumber").orElse("")

    doLast {
        val newVersion = pNewVersion.get()
        val newBuildNumber = pBuildNumber.get()

        if (newVersion.isBlank()) {
            logger.warn("Warning: -PnewVersion not provided.")
        }
        if (newBuildNumber.isBlank()) {
            logger.warn("Warning: -PbuildNumber not provided.")
        }

        if (newVersion.isBlank() && newBuildNumber.isBlank()) {
            return@doLast
        }

        // ---------------------------------------------------------
        // Update gradle.properties
        // ---------------------------------------------------------
        val propertiesFile = layout.projectDirectory.file("gradle.properties").asFile
        if (propertiesFile.exists()) {
            val lines = propertiesFile.readLines()
            val newLines = lines.map { line ->
                val trimmedLine = line.trim()
                when {
                    // Update Version Name
                    newVersion.isNotBlank() && trimmedLine.startsWith("version=") -> {
                        "version=$newVersion"
                    }
                    // Update Build Number (Version Code)
                    newBuildNumber.isNotBlank() && trimmedLine.startsWith("buildNumber=") -> {
                        "buildNumber=$newBuildNumber"
                    }
                    else -> line
                }
            }
            propertiesFile.writeText(newLines.joinToString("\n"))
            logger.lifecycle("Updated gradle.properties -> version: $newVersion, code: $newBuildNumber")
        }

        // ---------------------------------------------------------
        // Update iosApp/Configuration/Config.xcconfig
        // ---------------------------------------------------------
        val xcconfigFile = layout.projectDirectory.file("iosApp/Configuration/Config.xcconfig").asFile
        if (xcconfigFile.exists()) {
            val lines = xcconfigFile.readLines()
            val newLines = lines.map { line ->
                val trimmedLine = line.trim()
                when {
                    // iOS Version Number (MARKETING_VERSION)
                    newVersion.isNotBlank() && trimmedLine.startsWith("MARKETING_VERSION=") -> {
                        "MARKETING_VERSION=$newVersion"
                    }
                    // iOS Build Number (CURRENT_PROJECT_VERSION)
                    newBuildNumber.isNotBlank() && trimmedLine.startsWith("CURRENT_PROJECT_VERSION=") -> {
                        "CURRENT_PROJECT_VERSION=$newBuildNumber"
                    }
                    else -> line
                }
            }
            xcconfigFile.writeText(newLines.joinToString("\n"))
            logger.lifecycle("Updated Config.xcconfig -> MARKETING_VERSION: $newVersion, CURRENT_PROJECT_VERSION: $newBuildNumber")
        } else {
            logger.warn("Warning: iosApp/Configuration/Config.xcconfig not found!")
        }
    }
}
