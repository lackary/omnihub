plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.cocoapods) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.buildkonfig) apply false
}

tasks.register("setBuildVersion") {
    group = "versioning"
    description = "Updates the version and build number in gradle.properties and iosApp Config.xcconfig"

    // Receive -PnewVersion
    val pNewVersion = project.providers.gradleProperty("newVersion").orElse("")
    // Receive -PbuildNumber
    val pBuildNumber = project.providers.gradleProperty("buildNumber").orElse("")

    // Get file path (defined during configuration phase)
    val gradlePropertiesFile = layout.projectDirectory.file("gradle.properties")
    val xcconfigFile = layout.projectDirectory.file("iosApp/Configuration/Config.xcconfig")

    doLast {
        // Get value during execution phase
        val newVersion = pNewVersion.get()
        val newBuildNumber = pBuildNumber.get()

        // Use the logger inside the Task (this.logger), not the script's logger
        val taskLogger = this.logger

        if (newVersion.isBlank()) {
            taskLogger.warn("Warning: -PnewVersion not provided.")
        }
        if (newBuildNumber.isBlank()) {
            taskLogger.warn("Warning: -PbuildNumber not provided.")
        }

        if (newVersion.isBlank() && newBuildNumber.isBlank()) {
            return@doLast
        }

        // ---------------------------------------------------------
        // Update gradle.properties
        // ---------------------------------------------------------
        val propertiesFile = gradlePropertiesFile.asFile
        if (propertiesFile.exists()) {
            val lines = propertiesFile.readLines()
            val newLines = lines.map { line ->
                val trimmedLine = line.trim()
                when {
                    newVersion.isNotBlank() && trimmedLine.startsWith("version=") -> {
                        "version=$newVersion"
                    }
                    newBuildNumber.isNotBlank() && trimmedLine.startsWith("buildNumber=") -> {
                        "buildNumber=$newBuildNumber"
                    }
                    else -> line
                }
            }
            propertiesFile.writeText(newLines.joinToString("\n"))
            taskLogger.lifecycle("Updated gradle.properties -> version: $newVersion, code: $newBuildNumber")
        }

        // ---------------------------------------------------------
        // Update iosApp/Configuration/Config.xcconfig
        // ---------------------------------------------------------
        val xcFile = xcconfigFile.asFile
        if (xcFile.exists()) {
            val lines = xcFile.readLines()
            val newLines = lines.map { line ->
                val trimmedLine = line.trim()
                when {
                    newVersion.isNotBlank() && trimmedLine.startsWith("MARKETING_VERSION=") -> {
                        "MARKETING_VERSION=$newVersion"
                    }
                    newBuildNumber.isNotBlank() && trimmedLine.startsWith("CURRENT_PROJECT_VERSION=") -> {
                        "CURRENT_PROJECT_VERSION=$newBuildNumber"
                    }
                    else -> line
                }
            }
            xcFile.writeText(newLines.joinToString("\n"))
            taskLogger.lifecycle("Updated Config.xcconfig -> MARKETING_VERSION: $newVersion, CURRENT_PROJECT_VERSION: $newBuildNumber")
        } else {
            taskLogger.warn("Warning: iosApp/Configuration/Config.xcconfig not found!")
        }
    }
}
