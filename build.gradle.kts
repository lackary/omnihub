import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

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

tasks.named<Delete>("clean") {
    setDelete(emptySet<Any>())
    val rootBuildDir = layout.buildDirectory.get().asFile
    val rootLinkedPkg = layout.projectDirectory.dir("iosApp/KotlinMultiplatformLinkedPackage").asFile
    val sharedLinkedPkg = layout.projectDirectory.dir("shared/iosApp/KotlinMultiplatformLinkedPackage").asFile
    doLast {
        fun deleteSafely(file: File) {
            if (!file.exists()) return
            try {
                Files.walkFileTree(file.toPath(), object : SimpleFileVisitor<java.nio.file.Path>() {
                    override fun visitFile(f: java.nio.file.Path, attrs: BasicFileAttributes): FileVisitResult {
                        f.toFile().setWritable(true)
                        Files.deleteIfExists(f)
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: java.nio.file.Path, exc: IOException?): FileVisitResult {
                        dir.toFile().setWritable(true)
                        Files.deleteIfExists(dir)
                        return FileVisitResult.CONTINUE
                    }
                })
            } catch (_: Exception) {
                file.deleteRecursively()
            }
        }

        deleteSafely(rootBuildDir)
        deleteSafely(rootLinkedPkg)
        deleteSafely(sharedLinkedPkg)
    }
}

allprojects {
    tasks.matching { it.name == "cleanSwiftImportFingerprintArtifacts" }.configureEach {
        val deleteTask = this as? Delete ?: return@configureEach
        val syntheticDir = rootProject.layout.buildDirectory.dir("kotlin").get().asFile
        deleteTask.setDelete(emptySet<Any>())
        deleteTask.doLast {
            if (syntheticDir.exists()) {
                try {
                    java.nio.file.Files.walkFileTree(syntheticDir.toPath(), object : java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                        override fun visitFile(f: java.nio.file.Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                            f.toFile().setWritable(true)
                            java.nio.file.Files.deleteIfExists(f)
                            return java.nio.file.FileVisitResult.CONTINUE
                        }

                        override fun postVisitDirectory(dir: java.nio.file.Path, exc: java.io.IOException?): java.nio.file.FileVisitResult {
                            dir.toFile().setWritable(true)
                            java.nio.file.Files.deleteIfExists(dir)
                            return java.nio.file.FileVisitResult.CONTINUE
                        }
                    })
                } catch (_: Exception) {
                    syntheticDir.deleteRecursively()
                }
            }
        }
    }
}

