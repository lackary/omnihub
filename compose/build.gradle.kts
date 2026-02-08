import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import java.util.Properties

val modulePackageName = "io.lackstudio.omnihub.compose"
val unsplashAccessKeyName = "UNSPLASH_ACCESS_KEY"
val unsplashSecretKeyName = "UNSPLASH_SECRET_KEY"

// Read buildNumber, default to 1 if not provided (e.g. during development)
val buildNumberProp = project.findProperty("buildNumber") as? String
val appBuildNumber = buildNumberProp?.toIntOrNull() ?: 1

fun getFromLocalProperties(key: String, project: Project): String? {
    val file = project.rootProject.file("local.properties")
    if (!file.exists()) return null

    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return properties.getProperty(key)
}

fun resolveConfigValue(key: String, project: Project): String? {
    // Read from file first, if not found then read from environment variables
    return getFromLocalProperties(key, project) ?: System.getenv(key)
}

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hot.reload)
    alias(libs.plugins.kotlin.cocoapods)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildkonfig)
}

buildkonfig {
    packageName = modulePackageName

    val unsplashAccessKey = resolveConfigValue(unsplashAccessKeyName, project) ?: ""
    val unsplashSecretKey = resolveConfigValue(unsplashSecretKeyName, project) ?: ""

    defaultConfigs {
        buildConfigField(STRING, "APP_VERSION", project.version.toString()) // Automatically reads from gradle.properties)
        buildConfigField(STRING, "APP_BUILD_NUMBER", appBuildNumber.toString())
        buildConfigField(STRING, unsplashAccessKeyName, unsplashAccessKey)
        buildConfigField(STRING, unsplashSecretKeyName, unsplashSecretKey)
    }
}

kotlin {

    androidLibrary {
        namespace = modulePackageName
        compileSdk = libs.versions.android.targetSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() //  Opt-in to enable Java source compilation
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        // Set the Kotlin compilation target version
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }

        // Enable Android resource processing, default is false
        androidResources {
            enable = true
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

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "Compose" // This is the filename of prefix of podspec

        framework {
            // Required properties
            // Framework name configuration. Use this property instead of deprecated 'frameworkName'
            baseName = "Compose"

            // Optional properties
            // Specify the framework linking type. It's dynamic by default.
            isStatic = true
            // Dependency export
            // Uncomment and specify another project module if you have one:
            // export(project(":<your other KMP module>"))
//            transitiveExport = false // This is default.
        }

        // Maps custom Xcode configuration to NativeBuildType
//        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
//        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
    }
    
    jvm()

    // 'compose-webview-multiplatform' (kevinnzou) from omnifeed doesn't support JS.
    // Uncomment the following block if this dependency is no longer used.
//    js {
//        browser()
//        binaries.executable()
//    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui.tooling)
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
            implementation(libs.brys0.blurhash)
            implementation(libs.omnifeed)
            implementation(libs.omnifeed.core)
            implementation(libs.omnifeed.ui)
            implementation(libs.omnifeed.unsplash)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.cio)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

//android {
//    namespace = appPackageName
//    compileSdk = libs.versions.android.compileSdk.get().toInt()
//
//    defaultConfig {
//        applicationId = appPackageName
//        minSdk = libs.versions.android.minSdk.get().toInt()
//        targetSdk = libs.versions.android.targetSdk.get().toInt()
//        versionCode = appBuildNumber
//        versionName = project.version.toString()
//    }
//    packaging {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }
//    }
//    buildTypes {
//        getByName("release") {
//            isMinifyEnabled = false
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_21
//        targetCompatibility = JavaVersion.VERSION_21
//    }
//}

//dependencies {
//    debugImplementation(compose.uiTooling)
//}

compose.desktop {
    application {
        mainClass = "$modulePackageName.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.lackstudio.omnihub"
            packageVersion = project.version.toString()

            macOS {
                // If the project version starts with 0. (e.g. 0.2.0), DMG is forced to set to 1.0.0 to avoid errors
                // If the project version is already 1.0.0 or higher, use the project version directly
                val verStr = project.version.toString()
                dmgPackageVersion = if (verStr.startsWith("0.")) "1.0.0" else verStr

                macOS {
                    infoPlist {
                        extraKeysRawXml = """
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>io.lackstudio.omnihub</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>omnihub</string>
                                </array>
                            </dict>
                        </array>
                    """.trimIndent()
                    }
                }
            }
        }
    }
}

// Force exclusion of unstable JogAmp dependencies from the test Runtime Classpath
// This will not affect the app's production execution, only prevent tests from attempting to download it.
configurations.matching { it.name.contains("Test") }.configureEach {
    exclude(group = "org.jogamp.gluegen")
    exclude(group = "org.jogamp.jogl")
}
