#!/bin/bash

# ==========================================
# OmniHub - Act Local Testing Safe Script
# ==========================================

# 1. Security Check: Ensure execution from project root
if [ ! -f "gradlew" ] || [ ! -d ".github" ]; then
    echo "❌ Error: Please run this script from the project root (gradlew or .github folder not found)"
    exit 1
fi

echo "🚀 Preparing to run tests..."
echo "⚠️  Note: This will use your local Gradle and JDK environment"
echo ""

# 2. Menu: Let user choose which Workflow to run
echo "Please select the Workflow to test:"
echo "  1) Continuous Integration (main) [Uses ci_mikepenz.yml]"
echo "     - Simulates 'push' event using act"
echo "     - Fast, single Job"
echo ""
echo "  2) Continuous Integration (Dorny) [Uses ci_dorny.yml]"
echo "     - Simulates 'workflow_dispatch' event using act"
echo "     - Dual Jobs"
echo ""
echo "  3) Release Workflow (Container Mode) [Uses release.yml via act]"
echo "     - Simulates 'push' event inside Docker"
echo "     - May fail due to Docker npm network issues"
echo ""
echo "  4) Release Logic Check (Host Mode) [Direct npx]"
echo "     - Runs semantic-release directly on your Mac"
echo "     - Bypasses Docker issues. Best for checking logic."
echo ""
read -p "Enter option [1, 2, 3 or 4] (Default 1): " choice

# Default to 1
choice=${choice:-1}

read -p "Enable verbose logging (debug mode)? [y/N] " debug_resp
debug_resp=${debug_resp,,} # Convert to lowercase
VERBOSE_FLAG=""
if [[ "$debug_resp" =~ ^(yes|y)$ ]]; then
    VERBOSE_FLAG="-v"
    echo "🐞 Debug mode enabled."
fi

echo ""
echo "------------------------------------------"

# Set a local temporary path to simulate the artifact server
ARTIFACT_PATH="/tmp/act-artifacts"
mkdir -p "$ARTIFACT_PATH"

# 👇 Added this line to define the Log file location
LOG_FILE="act_execution.log"

# Ensure user has gh cli installed, otherwise prompt
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI (gh) not detected. act/npx may fail due to missing Token."
    echo "Recommendation: brew install gh"
    TOKEN_ARG=""
    EXPORT_TOKEN=""
else
    # Automatically fetch the currently logged-in Token
    RAW_TOKEN=$(gh auth token)
    TOKEN_ARG="-s GITHUB_TOKEN=$RAW_TOKEN"
    EXPORT_TOKEN=$RAW_TOKEN
fi

if [ "$choice" == "1" ]; then
    echo "🔵 Running: Main Workflow (Mike Penz)..."
    CMD="act push -W .github/workflows/ci_mikepenz.yml -P macos-latest=-self-hosted --artifact-server-path \"$ARTIFACT_PATH\" $TOKEN_ARG $VERBOSE_FLAG"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "2" ]; then
    echo "🟠 Running: Dorny Workflow (Manual)..."
    CMD="act workflow_dispatch -W .github/workflows/ci_dorny.yml -P macos-latest=-self-hosted -P ubuntu-latest=catthehacker/ubuntu:act-latest --artifact-server-path \"$ARTIFACT_PATH\" $TOKEN_ARG $VERBOSE_FLAG"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "3" ]; then
    echo "🟣 Running: Release Workflow (Container Mode)..."
    echo "⚠️  Note: Running inside Docker container."
    CMD="act push -W .github/workflows/release.yml -P macos-latest=-self-hosted $TOKEN_ARG $VERBOSE_FLAG"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "4" ]; then
    echo "🟢 Running: Release Logic Check (Host Mode)..."
    echo "⚡ This runs directly on your machine using npx."

    # Check if npm/npx is installed
    if ! command -v npx &> /dev/null; then
        echo "❌ Error: npx is not installed. Please install Node.js."
        exit 1
    fi

    # Export token for this session only
    export GITHUB_TOKEN=$EXPORT_TOKEN

    # Simulate GITHUB_RUN_NUMBER to allow local tests to run smoothly
    if [ -z "$GITHUB_RUN_NUMBER" ]; then
        echo "⚠️  GITHUB_RUN_NUMBER is not set. Using '9999' for local test."
        export GITHUB_RUN_NUMBER=9999
    fi

    # Execute npx and install extra plugins explicitly because .releaserc.yml requires them
    CMD="npx -p semantic-release -p @semantic-release/git -p @semantic-release/changelog -p @semantic-release/exec semantic-release --dry-run --branches main --no-ci"
    echo "👉 Executing: $CMD"

    # Run semantic-release dry-run
    eval $CMD

    ACT_EXIT_CODE=$?

else
    echo "❌ Invalid option, script terminated."
    exit 1
fi

echo ""
echo "=========================================="

if [ $ACT_EXIT_CODE -eq 0 ]; then
    echo "✅ Process completed successfully!"

    if [ "$choice" == "2" ]; then
        echo "ℹ️  (Dorny Mode Tip) Please check 'build/test-results'."
    fi
    if [ "$choice" == "4" ]; then
        echo "ℹ️  (Logic Check Tip) Scroll up to see the Dry Run logs."
        echo "    Look for 'The next release version is...' or 'No relevant changes'."
    fi
else
    echo "❌ Process failed (Exit Code: $ACT_EXIT_CODE)"
fi
echo "=========================================="

# 3. Safety Cleanup Mechanism
echo ""
read -p "🧹 Do you want to clean Gradle build artifacts? [y/N] " response
response=${response,,}
if [[ "$response" =~ ^(yes|y)$ ]]; then
    ./gradlew clean
    echo "✨ Cleanup complete!"
else
    echo "👌 Build files retained."
fi

exit $ACT_EXIT_CODE
