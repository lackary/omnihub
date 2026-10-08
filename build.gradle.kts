import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import java.io.File
import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    base
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.hot.reload) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.gms.google.services) apply false
}

plugins.withType<YarnPlugin> {
    the<YarnRootExtension>().apply {
        yarnLockMismatchReportProperty.set(YarnLockMismatchReport.WARNING)
        yarnLockAutoReplaceProperty.set(true)
    }
}

subprojects {
    afterEvaluate {
        val pSkipLint = providers.gradleProperty("skip.lint").orNull?.toBoolean() == true
        val pSkipTests = providers.gradleProperty("skip.tests").orNull?.toBoolean() == true
        val pSkipNativeTests = providers.gradleProperty("skip.native.tests").orNull?.toBoolean() == true

        if (pSkipLint) {
            tasks.matching { it.name.contains(Regex("lint", RegexOption.IGNORE_CASE)) }.configureEach {
                enabled = false
            }
        }

        if (pSkipTests) {
            tasks.withType<Test>().configureEach {
                enabled = false
            }
        }

        if (pSkipNativeTests) {
            tasks.withType<KotlinNativeTest>().configureEach {
                enabled = false
            }
            tasks.withType<KotlinNativeLink>().configureEach {
                enabled = false
            }
        }
    }

    // Ensure that all JS & WasmJs test tasks across every subproject module
    // explicitly depend on all NPM install and Wasm tooling setup tasks completing first.
    tasks.withType<KotlinJsTest>().configureEach {
        dependsOn(rootProject.tasks.matching {
            it.name.endsWith("NpmInstall") || it.name.endsWith("ToolingSetup")
        })
    }
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

abstract class CleanTaskHelper @Inject constructor(
    private val execOperations: ExecOperations
) {
    fun cleanDirs(vararg paths: String) {
        val existing = paths.filter { File(it).exists() }
        if (existing.isNotEmpty()) {
            execOperations.exec {
                commandLine("rm", "-rf", *existing.toTypedArray())
                isIgnoreExitValue = true
            }
        }
    }
}

val rootBuildDirFile = layout.buildDirectory.get().asFile
val rootLinkedPkgFile = layout.projectDirectory.dir("iosApp/KotlinMultiplatformLinkedPackage").asFile
val sharedLinkedPkgFile = layout.projectDirectory.dir("shared/iosApp/KotlinMultiplatformLinkedPackage").asFile
val rootKotlinBuildDirFile = layout.buildDirectory.dir("kotlin").get().asFile

tasks.named<Delete>("clean") {
    setDelete(emptySet<Any>())
    val helper = objects.newInstance<CleanTaskHelper>()
    val buildPath = rootBuildDirFile.absolutePath
    val rootPkgPath = rootLinkedPkgFile.absolutePath
    val sharedPkgPath = sharedLinkedPkgFile.absolutePath
    doFirst {
        helper.cleanDirs(buildPath, rootPkgPath, sharedPkgPath)
    }
}

allprojects {
    tasks.matching { it.name == "cleanSwiftImportFingerprintArtifacts" }.configureEach {
        (this as? Delete)?.setDelete(emptySet<Any>())
        val helper = objects.newInstance<CleanTaskHelper>()
        val kotlinBuildPath = rootKotlinBuildDirFile.absolutePath
        doFirst {
            helper.cleanDirs(kotlinBuildPath)
        }
    }
}

