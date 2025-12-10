#!/bin/bash
# This script initializes or fixes the CocoaPods configuration for the iOS project, solving the "chicken-and-egg" problem.

# Ensure the script runs from the project root directory
set -e
cd "$(dirname "$0")"

# --- Step 1: Clean old build caches ---
# This prevents old or incorrect resources/paths from affecting the new build.
echo "🧹 [1/4] Cleaning previous builds..."
./gradlew clean

# --- Step 2: Create an empty resource directory ---
# This is the key to the whole process!
# We manually create an empty directory just so `pod install` won't error out when checking spec.resources.
# This ensures the [CP] Copy Pods Resources script is correctly added to the Xcode project.
echo "📁 [2/4] Creating dummy resource directory for CocoaPods..."
mkdir -p composeApp/build/compose/cocoapods/compose-resources

# --- Step 3: Generate a dummy Framework ---
# This is also to "trick" CocoaPods. The Podfile needs to see the .framework file exists to run.
echo "⚙️ [3/4] Generating dummy framework for CocoaPods..."
./gradlew :composeApp:generateDummyFramework

# --- Step 4: Run Pod Install ---
# Now, because both the empty directory and the dummy Framework exist, pod install can finish successfully.
echo "📦 [4/4] Installing Pods..."
cd iosApp
pod install --repo-update --clean-install

echo "✅ Setup complete! You can now open 'iosApp.xcworkspace' in Xcode and run the project."
