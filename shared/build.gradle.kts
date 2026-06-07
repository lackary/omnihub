@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.util.Base64
import java.util.Properties

val modulePackageName = "io.lackstudio.omnihub.shared"
val unsplashAccessKeyName = "UNSPLASH_ACCESS_KEY"
val unsplashSecretKeyName = "UNSPLASH_SECRET_KEY"
val firebaseAndroidBase64Name = "FIREBASE_ANDROID_BASE64"
val firebaseIosBase64Name = "FIREBASE_IOS_BASE64"
val firebaseWebBase64Name = "FIREBASE_WEB_BASE64"
val googleServerClientIdName = "GOOGLE_SERVER_CLIENT_ID"

// Read buildNumber, default to 1 if not provided (e.g. during development)
val buildNumberProp = project.findProperty("buildNumber") as? String
val appBuildNumber = buildNumberProp?.toIntOrNull() ?: 1

fun getFromPropertiesFile(fileName: String, key: String, project: Project): String? {
    val file = project.rootProject.file(fileName)
    if (!file.exists()) return null

    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties.getProperty(key)
}

fun resolveConfigValue(key: String, project: Project): String? {
    // Priority: .secrets -> Environment variables
    return getFromPropertiesFile(".secrets", key, project)
//        ?: getFromPropertiesFile("local.properties", key, project)
        ?: System.getenv(key)
}

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.buildkonfig)
}

buildkonfig {
    packageName = modulePackageName

    val unsplashAccessKey = resolveConfigValue(unsplashAccessKeyName, project) ?: ""
    val unsplashSecretKey = resolveConfigValue(unsplashSecretKeyName, project) ?: ""
    val firebaseAndroidBase64 = resolveConfigValue(firebaseAndroidBase64Name, project) ?: ""
    val firebaseIosBase64 = resolveConfigValue(firebaseIosBase64Name, project) ?: ""
    val firebaseWebBase64 = resolveConfigValue(firebaseWebBase64Name, project) ?: ""

    // 1. Automatically decode from Android Base64 and extract Web Client ID (client_type: 3)
    val googleServerClientId = if (firebaseAndroidBase64.isNotEmpty()) {
        try {
            val decodedBytes = Base64.getDecoder().decode(firebaseAndroidBase64)
            val decoded = String(decodedBytes)
            // Find the client_id before client_type: 3
            // The format is usually "client_id": "...", "client_type": 3
            val regex = "\"client_id\":\\s*\"([^\"]+)\",\\s*\"client_type\":\\s*3".toRegex()
            regex.find(decoded)?.groupValues?.get(1) ?: ""
        } catch (e: Exception) { "" }
    } else ""

    val isDebug = System.getenv("CONFIGURATION") == "Debug" ||
            project.gradle.startParameter.taskNames.any { task ->
                task.contains("debug", ignoreCase = true) ||
                task.contains("run", ignoreCase = true) ||
                task.contains("Development", ignoreCase = true)
            }
    val defaultAppName = if (isDebug) "OmniHub Dev" else "OmniHub"

    defaultConfigs {
        buildConfigField(STRING, "APP_NAME", defaultAppName)
        buildConfigField(STRING, "APP_VERSION", project.version.toString())
        buildConfigField(STRING, "APP_BUILD_NUMBER", appBuildNumber.toString())
        buildConfigField(STRING, unsplashAccessKeyName, unsplashAccessKey)
        buildConfigField(STRING, unsplashSecretKeyName, unsplashSecretKey)
        buildConfigField(STRING, firebaseAndroidBase64Name, firebaseAndroidBase64)
        buildConfigField(STRING, firebaseIosBase64Name, firebaseIosBase64)
        buildConfigField(STRING, firebaseWebBase64Name, firebaseWebBase64)
        buildConfigField(STRING, googleServerClientIdName, googleServerClientId)
    }
    targetConfigs {
        create("debug") {
            buildConfigField(STRING, "APP_NAME", "OmniHub Dev")
        }
        create("release") {
            buildConfigField(STRING, "APP_NAME", "OmniHub")
        }
    }
}

kotlin {

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = modulePackageName
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Set the Kotlin compilation target version
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }

        // Enable Android resource processing, default is false
        androidResources {
            enable = true
        }

        withJava() //  Opt-in to enable Java source compilation
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest {
        }
    }
    
//    listOf(
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
//        }
//    }
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        // Required properties
        // Specify the required Pod version here
        // Otherwise, the Gradle project version is used
        version = project.version.toString()
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"
        ios.deploymentTarget = "18.2"
        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "Shared" // This is the filename of prefix of podspec

        framework {
            // Required properties
            // Framework name configuration. Use this property instead of deprecated 'frameworkName'
            baseName = "Shared"

            // Optional properties
            // Specify the framework linking type. It's dynamic by default.
            isStatic = true
            // Dependency export
            // Uncomment and specify another project module if you have one:
            // export(project(":<your other KMP module>"))
            transitiveExport = false // This is default.
            export(libs.omnifeed.auth)
        }

        pod("FirebaseCore") {
            version = "~> 12.4.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("FirebaseAuth") {
            version = "~> 12.4.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("GoogleSignIn") {
            version = "~> 9.0.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        // Maps custom Xcode configuration to NativeBuildType
//        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
//        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }
    
    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        compilerOptions {
            freeCompilerArgs.add("-opt-in=kotlin.js.ExperimentalWasmJsInterop")
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.ui.tooling)
            implementation(libs.androidx.xr.runtime)
            implementation(libs.androidx.xr.scenecore)
            implementation(libs.androidx.xr.compose)
            implementation(libs.androidx.xr.material3)
            implementation(libs.ktor.client.android)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.google.gms.play.service.auth)
            implementation(libs.google.googleid)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(project.dependencies.platform(libs.google.firebase.bom))
        }
//        androidHostTest.dependencies {
//            implementation(libs.robolectric)
//            implementation(libs.androidx.compose.ui.test.junit4)
//            implementation(libs.androidx.test.ext.junit)
//        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.androidx.compose.ui.test.junit4)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.compose.ui.test.manifest)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3.adaptive.navigation.suite)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.touchlab.kermit)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.panpf.sketch.compose)
            implementation(libs.panpf.sketch.http)
            implementation(libs.panpf.sketch.blurhash)
            implementation(libs.omnifeed)
            implementation(libs.omnifeed.core)
            implementation(libs.omnifeed.ui)
            implementation(libs.omnifeed.unsplash)
            api(libs.omnifeed.auth)
        }
        commonTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlin.wrappers.browser)
        }
        jsMain.dependencies {
            implementation(libs.kotlin.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}

// Force exclusion of unstable JogAmp dependencies from the test Runtime Classpath
// This will not affect the app's production execution, only prevent tests from attempting to download it.
configurations.matching { it.name.contains("Test") }.configureEach {
    exclude(group = "org.jogamp.gluegen")
    exclude(group = "org.jogamp.jogl")
}
