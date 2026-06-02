import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(projects.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.gitlive.firebase.auth)
    implementation(libs.omnifeed.auth)
}

compose.desktop {
    application {
        mainClass = "io.lackstudio.omnihub.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.lackstudio.omnihub"
            packageVersion = "1.0.0"

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
