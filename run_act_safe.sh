#!/bin/bash

# ==========================================
# OmniHub - Act Local Testing Safe Script
# ==========================================

# 1. Security Check: Ensure execution from project root
if [ ! -f "gradlew" ] || [ ! -d ".github" ]; then
    echo "❌ Error: Please run this script from the project root (gradlew or .github folder not found)"
    exit 1
fi

echo "🚀 Preparing to run act (macOS local mode)..."
echo "⚠️  Note: This will use your local Gradle and JDK environment"
echo ""

# 2. Menu: Let user choose which Workflow to run
echo "Please select the Workflow to test:"
echo "  1) Continuous Integration (main) [Uses ci_mikepenz.yml]"
echo "     - Simulates 'push' event"
echo "     - Fast, single Job"
echo ""
echo "  2) Continuous Integration (Dorny) [Uses ci_dorny.yml]"
echo "     - Simulates 'workflow_dispatch' event"
echo "     - Dual Jobs (Local will run the first XML generation part; the second Linux Job might fail or be skipped)"
echo ""
read -p "Enter option [1 or 2] (Default 1): " choice

# Default to 1
choice=${choice:-1}

echo ""
echo "------------------------------------------"

# Set a local temporary path to simulate the artifact server
ARTIFACT_PATH="/tmp/act-artifacts"
mkdir -p "$ARTIFACT_PATH"

# Ensure user has gh cli installed, otherwise prompt
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI (gh) not detected. act may fail due to missing Token."
    echo "Recommendation: brew install gh"
    TOKEN_ARG=""
else
    # Automatically fetch the currently logged-in Token
    TOKEN_ARG="-s GITHUB_TOKEN=$(gh auth token)"
fi

if [ "$choice" == "1" ]; then
    echo "🔵 Running: Main Workflow (Mike Penz)..."
    # -W specifies the specific workflow file
    act push -W .github/workflows/ci_mikepenz.yml -P macos-latest=-self-hosted --artifact-server-path "$ARTIFACT_PATH" $TOKEN_ARG
    ACT_EXIT_CODE=$?

elif [ "$choice" == "2" ]; then
    echo "🟠 Running: Dorny Workflow (Manual)..."
    # Dorny is manually triggered, so we use workflow_dispatch
    # Note: Dorny's second Job requires Linux docker. Handling multi-platform in act's local Host mode is tricky.
    # Here we primarily verify if the XML is successfully generated.
    # Added -P ubuntu-latest=catthehacker/ubuntu:act-latest
    # This tells act: When YAML specifies runs-on: ubuntu-latest, use this full-featured image instead of the default slim image
    act workflow_dispatch \
        -W .github/workflows/ci_dorny.yml \
        -P macos-latest=-self-hosted \
        -P ubuntu-latest=catthehacker/ubuntu:act-latest \
        --artifact-server-path "$ARTIFACT_PATH" \
        $TOKEN_ARG

    ACT_EXIT_CODE=$?
else
    echo "❌ Invalid option, script terminated."
    exit 1
fi

echo ""
echo "=========================================="

if [ $ACT_EXIT_CODE -eq 0 ]; then
    echo "✅ CI process completed successfully!"

    # If running Dorny, remind to check XML
    if [ "$choice" == "2" ]; then
        echo "ℹ️  (Dorny Mode Tip) Please check 'build/test-results' to see if XML files were generated."
        echo "    Note: Local act might not perfectly execute the second stage Docker report generation; if XML exists, it counts as success."
    fi
else
    echo "❌ CI process failed (Exit Code: $ACT_EXIT_CODE)"
fi
echo "=========================================="

# 3. Safety Cleanup Mechanism
echo ""
read -p "🧹 Do you want to clean Gradle build artifacts (run ./gradlew clean) to free up space? [y/N] " response

# Convert input to lowercase
response=${response,,}

if [[ "$response" =~ ^(yes|y)$ ]]; then
    echo "🧹 Cleaning up..."
    ./gradlew clean
    echo "✨ Cleanup complete!"
else
    echo "👌 Build files retained (next build will be faster)."
fi

exit $ACT_EXIT_CODE
