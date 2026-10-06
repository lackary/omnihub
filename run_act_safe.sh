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

# Configure Artifact Paths
ARTIFACT_PATH="./build/act-artifacts"
CACHE_PATH="./build/act-cache"

mkdir -p "$ARTIFACT_PATH" "$CACHE_PATH"

# Safety check: Remove global legacy cache if it exists
if [ -d "$HOME/.cache/act" ]; then
    echo "🧹 Found legacy cache at ~/.cache/act. Removing it to prevent conflicts..."
    rm -rf "$HOME/.cache/act"
fi

# ==============================================================================
# 🔑 Secrets & Token Management (Merge Strategy)
# ==============================================================================
USER_SECRETS=".secrets"       # Manual secrets
RUN_SECRETS=".secrets.run"    # Temp file for execution

: > "$RUN_SECRETS"

if [ -f "$USER_SECRETS" ]; then
    echo "📝 Loading keys from $USER_SECRETS..."
    cat "$USER_SECRETS" >> "$RUN_SECRETS"
    echo "" >> "$RUN_SECRETS"
else
    echo "⚠️  $USER_SECRETS not found. Creating a template..."
    cat <<EOF > "$USER_SECRETS"
UNSPLASH_ACCESS_KEY=dummy_val
UNSPLASH_SECRET_KEY=dummy_val
GOOGLE_SERVICES_WEB_CLIENT_ID=dummy_val
GOOGLE_CLIENT_ID=dummy_val
GOOGLE_REVERSED_CLIENT_ID=dummy_val
EOF
    cat "$USER_SECRETS" >> "$RUN_SECRETS"
    echo "" >> "$RUN_SECRETS"
    echo "⚠️  Template created. Some tests may fail without real keys."
fi

LOG_FILE="act_execution.log"

# Retrieve Token from 'gh' and append to temp file
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI (gh) not detected. Release steps will fail."
    EXPORT_TOKEN=""
else
    RAW_TOKEN=$(gh auth token 2>/dev/null)
    if [ -n "$RAW_TOKEN" ]; then
        echo "✅ GitHub Token auto-detected from 'gh'."
        echo "# --- Dynamic Tokens ---" >> "$RUN_SECRETS"
        echo "GITHUB_TOKEN=$RAW_TOKEN" >> "$RUN_SECRETS"
        echo "SEMANTIC_RELEASE_TOKEN=$RAW_TOKEN" >> "$RUN_SECRETS"
        EXPORT_TOKEN=$RAW_TOKEN
    else
        echo "⚠️  gh is installed but not logged in."
        EXPORT_TOKEN=""
    fi
fi

# ==============================================================================
# Menu: Let user choose which Workflow to run
# ==============================================================================
echo ""
echo "Please select the Workflow to test:"
echo "  1) Full Continuous Integration (.github/workflows/ci.yml)"
echo "     - Runs all jobs (test, build-android, build-web, build-desktop, build-ios)"
echo ""
echo "  2) KMP Unit Tests Only (.github/workflows/ci.yml -j test)"
echo "     - Fast local check for unit tests"
echo ""
echo "  3) Release Workflow (.github/workflows/release.yml)"
echo "     - Simulates semantic-release via act"
echo ""
echo "  4) Release Logic Check (Host Mode)"
echo "     - Runs semantic-release directly on your Mac using npx"
echo ""
read -p "Enter option [1, 2, 3 or 4] (Default 1): " choice
choice=${choice:-1}

read -p "Enable verbose logging (debug mode)? [y/N] " debug_resp
debug_resp=$(echo "$debug_resp" | tr '[:upper:]' '[:lower:]')
VERBOSE_FLAG=""
if [[ "$debug_resp" =~ ^(yes|y)$ ]]; then
    VERBOSE_FLAG="-v"
    echo "🐞 Debug mode enabled."
fi

# Self-Hosted Mode Configuration: Map runner environments to local host execution
unset ANDROID_PREFS_ROOT
ACT_COMMON_ARGS="--platform macos-latest=-self-hosted \
--platform macos-26=-self-hosted \
--platform ubuntu-latest=-self-hosted \
--env ACT=true \
--env ANDROID_PREFS_ROOT= \
--secret-file \"$RUN_SECRETS\" \
--artifact-server-path \"$ARTIFACT_PATH\" \
--cache-server-path \"$CACHE_PATH\" \
$VERBOSE_FLAG"

echo ""
echo "------------------------------------------"

if [ "$choice" == "1" ]; then
    echo "🔵 Running: Full Continuous Integration..."
    CMD="act push -W .github/workflows/ci.yml $ACT_COMMON_ARGS"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "2" ]; then
    echo "🔵 Running: KMP Unit Tests Only (-j test)..."
    CMD="act push -W .github/workflows/ci.yml -j test $ACT_COMMON_ARGS"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "3" ]; then
    echo "🟣 Running: Release Workflow (Container Mode)..."
    echo "⚠️  [SAFETY CHECK] You are about to run the Release Workflow locally."
    echo ""
    read -p "❓ Do you want to proceed? (y/N) " confirm
    if [[ ! "$confirm" =~ ^(yes|y)$ ]]; then
        echo "🚫 Aborted by user."
        rm -f "$RUN_SECRETS" 2>/dev/null
        exit 0
    fi

    CMD="act push -W .github/workflows/release.yml $ACT_COMMON_ARGS"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "4" ]; then
    echo "🟢 Running: Release Logic Check (Host Mode)..."
    echo "⚡ This runs directly on your machine using npx."

    if ! command -v npm &> /dev/null; then
        echo "❌ Error: npm/Node.js is not installed."
        rm -f "$RUN_SECRETS" 2>/dev/null
        exit 1
    fi

    export GITHUB_TOKEN=$EXPORT_TOKEN

    if [ -z "$GITHUB_RUN_NUMBER" ]; then
        echo "⚠️  GITHUB_RUN_NUMBER is not set. Using '9999' for local test."
        export GITHUB_RUN_NUMBER=9999
    fi

    if [ ! -d "node_modules" ]; then
        echo "📦 Installing npm dependencies..."
        npm install
    fi

    echo "⚡ Executing semantic-release..."
    npx semantic-release --dry-run --branches "$(git branch --show-current)" --no-ci
    ACT_EXIT_CODE=$?

else
    echo "❌ Invalid option, script terminated."
    rm -f "$RUN_SECRETS" 2>/dev/null
    exit 1
fi

rm -f "$RUN_SECRETS" 2>/dev/null

echo ""
echo "=========================================="

if [ $ACT_EXIT_CODE -eq 0 ]; then
    echo "✅ Process completed successfully!"
    if [ "$choice" == "2" ]; then
        echo "ℹ️  (Unit Test Tip) Check build/test-results or act_execution.log for details."
    fi
    if [ "$choice" == "4" ]; then
        echo "ℹ️  (Logic Check Tip) Scroll up to see the Dry Run logs."
    fi
else
    echo "❌ Process failed (Exit Code: $ACT_EXIT_CODE)"
fi
echo "=========================================="

# Safety Cleanup Mechanism
echo ""
read -p "🧹 Do you want to clean Gradle build artifacts and act cache? [y/N] " response
response=$(echo "$response" | tr '[:upper:]' '[:lower:]')
if [[ "$response" =~ ^(yes|y)$ ]]; then
    ./gradlew clean
    rm -rf "$ARTIFACT_PATH" "$CACHE_PATH"
    echo "✨ Cleanup complete!"
else
    echo "👌 Build files retained."
fi

exit $ACT_EXIT_CODE
