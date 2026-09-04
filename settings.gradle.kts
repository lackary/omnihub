rootProject.name = "OmniHub"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Required for 'compose-webview-multiplatform' (kevinnzou) from omnifeed in Desktop platform.
        // Remove this repository if the dependency is no longer used.
        maven("https://jogamp.org/deployment/maven/")
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Required for 'compose-webview-multiplatform' (kevinnzou) from omnifeed in Desktop platform.
        // Remove this repository if the dependency is no longer used.
        maven("https://jogamp.org/deployment/maven/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":shared")
include(":androidApp")
include(":desktopApp")
include(":webApp")

// =========== Composite Build ===========

// Define the relative path of omnifeed-kmp for local or CI environment
// During local development, these two projects are usually siblings under the same parent directory
val omnifeedProjectDir = file("../omnifeed-kmp")
val isOmniFeedLocalExists = omnifeedProjectDir.exists()

// Read environment variable (for CI use, ensuring CI executes this logic)
val forceCompositeBuild = System.getenv("FORCE_COMPOSITE_BUILD") == "true"

if (isOmniFeedLocalExists || forceCompositeBuild) {
    println("⚙️ [Gradle] Detecting local 'omnifeed-kmp', enabling Composite Build...")

    includeBuild(omnifeedProjectDir) {
        dependencySubstitution {
            // Substitute Core module
            substitute(module("io.lackstudio.omnifeed:omnifeed"))
                .using(project(":omnifeed"))
            // Substitute Core module
            substitute(module("io.lackstudio.omnifeed:omnifeed-core"))
                .using(project(":omnifeed-core"))

            // Substitute Unsplash integration module
            substitute(module("io.lackstudio.omnifeed:omnifeed-unsplash"))
                .using(project(":omnifeed-unsplash"))

            // Substitute UI module
            substitute(module("io.lackstudio.omnifeed:omnifeed-ui"))
                .using(project(":omnifeed-ui"))

            // Substitute Auth module
            substitute(module("io.lackstudio.omnifeed:omnifeed-auth"))
                .using(project(":omnifeed-auth"))
        }
    }
} else {
    println("⚠️ [Gradle] 'omnifeed-kmp' not found locally. Using binary dependencies from Maven.")
}

