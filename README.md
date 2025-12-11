This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:
- for the Wasm target (faster, modern browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
    ```
- for the JS target (slower, supports older browsers):
  - on macOS/Linux
    ```shell
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
  - on Windows
    ```shell
    .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
    ```

### Configure CocoaPods Integration

This project uses the Kotlin Multiplatform CocoaPods plugin to handle iOS dependencies.

**Prerequisites:**
1. Install [CocoaPods](https://cocoapods.org/) if you haven't already:

   ```shell
   sudo gem install cocoapods
   ```

   or use `Homebrew` if you have it installed:

   ```shell
   brew install cocoapods
   ```
   
2. In the `gradle/libs.versions.toml` file, add the Kotlin CocoaPods Gradle plugin to the `[plugins]` block:

   ```toml
   kotlinNativeCocoapods = { id = "org.jetbrains.kotlin.native.cocoapods", version.ref = "kotlin" }
   ```
   
3. Navigate to the root `build.gradle.kts` file of your project and add the following alias to the `plugins {}` block:

   ```kotlin
   alias(libs.plugins.kotlinNativeCocoapods) apply false
   ```
4. Open the module where you want to integrate CocoaPods (e.g., the `composeApp` module), and add the following alias to the `plugins {}` block of the `build.gradle.kts` file:

   ```kotlin
   alias(libs.plugins.kotlinNativeCocoapods)
   ```

**Setup:**
Follow these steps for initial setup or after modifying dependencies:

1. Navigate to the `iosApp` directory:

   ```shell
   cd iosApp
   ```
2. Initialize the pods:

   ```shell
   pod init
   ```
   This generates the `Podfile`.

3. In `composeApp/build.gradle.kts`, configure the version, summary, homepage, and baseName of the Podspec file in the `cocoapods` block within the `kotlin` block:
   
   ```kotlin
   iosArm64()
   iosSimulatorArm64()
   
   cocoapods {
        // Required properties
        // Specify the required Pod version here
        // Otherwise, the Gradle project version is used
        version = "1.0.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "ComposeApp"

        framework {
            // Required properties
            // Framework name configuration. Use this property instead of deprecated 'frameworkName'
            baseName = "ComposeApp"

            // Optional properties
            // Specify the framework linking type. It's dynamic by default.
            isStatic = true
            // Dependency export
            // Uncomment and specify another project module if you have one:
            // export(project(":<your other KMP module>"))
            //transitiveExport = false // This is default.
        }

        // Maps custom Xcode configuration to NativeBuildType
        //xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        //xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE
   }
   ```
   
   Then run **File | Sync Project with Gradle Files** in Android Studio to reimport the project. This process will generate the Podspec file for the project.

4. Update the `Podfile` by adding the following line below `# Pods for iosApp`:

   ```ruby
   pod 'ComposeApp', :path => '../composeApp'
   ```

5. Run the helper script setup_ios.sh. This script automates the following tasks:

   - `./gradlew clean`
   - Create the directory `composeApp/build/compose/cocoapods/compose-resources`
   - `./gradlew :composeApp:generateDummyFramework` (generates a dummy framework in `build/cocoapods/framework/ComposeApp.framework`).
   - Move to `iosApp` and run `pod install --repo-update --clean-install`. This generates the `iosApp.xcworkspace` file, which includes the `ComposeApp` module.

6. You will see a notification to **reload project as workspace**. This is **important**; the iOS application build will fail if you do not reload.

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Local CI Testing

This project uses GitHub Actions for CI/CD. To test the workflows locally without pushing to GitHub, we use [act](https://github.com/nektos/act) along with a helper script.

**Prerequisites:**
1. Install [Docker](https://www.docker.com/).
2. Install `act` (e.g., `brew install act`).
3. Install GitHub CLI `gh` (e.g., `brew install gh`) for authentication.

**How to run:**
We provide a safe wrapper script to handle artifact paths and permissions automatically.

```shell
shell ./run_act_safe.sh
```

This script provides an interactive menu to choose between:
*   **Main Workflow (Mike Penz):** Simulates a `push` event. Fast, single-job routine for daily feedback.
*   **Dorny Workflow (Manual):** Simulates a `workflow_dispatch` event. Multi-job routine for generating detailed standalone reports.

> **Note:** The script will ask for your permission to clean up Gradle build artifacts after execution to save disk space.


---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
