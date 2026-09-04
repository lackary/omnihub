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
debug_resp=$(echo "$debug_resp" | tr '[:upper:]' '[:lower:]') # Convert to lowercase
VERBOSE_FLAG=""
if [[ "$debug_resp" =~ ^(yes|y)$ ]]; then
    VERBOSE_FLAG="-v"
    echo "🐞 Debug mode enabled."
fi

echo ""
echo "------------------------------------------"

# Set a local temporary path to simulate the artifact server
# Move artifacts & cache to build directory
ARTIFACT_PATH="./build/act-artifacts"
CACHE_PATH="./build/act-cache"

# Redirect act's internal download cache (for actions like checkout@v4) to build/
# This ensures ~/.cache/act is NOT used/polluted.
export XDG_CACHE_HOME="$(pwd)/build/act-xdg-cache"

# Create directories if they don't exist
mkdir -p "$ARTIFACT_PATH"
mkdir -p "$CACHE_PATH"
mkdir -p "$XDG_CACHE_HOME"

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
fi

# Added this line to define the Log file location
LOG_FILE="act_execution.log"

# Ensure user has gh cli installed, otherwise prompt
if ! command -v gh &> /dev/null; then
    echo "⚠️  GitHub CLI (gh) not detected. Using local git history for dry-run checks."
    EXPORT_TOKEN=""
else
    RAW_TOKEN=$(gh auth token 2>/dev/null)
    if [ -n "$RAW_TOKEN" ]; then
        echo "✅ GitHub Token auto-detected from 'gh'."
        echo "# --- Dynamic Tokens ---" >> "$RUN_SECRETS"
        echo "GITHUB_TOKEN=$RAW_TOKEN" >> "$RUN_SECRETS"
        echo "SEMANTIC_RELEASE_TOKEN=$RAW_TOKEN" >> "$RUN_SECRETS"
        EXPORT_TOKEN=$RAW_TOKEN
    fi
fi

if [ "$choice" == "1" ]; then
    echo "🔵 Running: Main Workflow (Mike Penz)..."
    CMD="act push \
      -W .github/workflows/ci_mikepenz.yml \
      -P macos-latest=-self-hosted \
      --env ACT=true \
      --secret-file \"$RUN_SECRETS\" \
      --artifact-server-path \"$ARTIFACT_PATH\" \
      --cache-server-path \"$CACHE_PATH\" \
      $VERBOSE_FLAG"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "2" ]; then
    echo "🟠 Running: Dorny Workflow (Manual)..."
    CMD="act workflow_dispatch \
      -W .github/workflows/ci_dorny.yml \
      -P macos-latest=-self-hosted \
      -P ubuntu-latest=catthehacker/ubuntu:act-latest \
      --env ACT=true \
      --secret-file \"$RUN_SECRETS\" \
      --artifact-server-path \"$ARTIFACT_PATH\" \
      --cache-server-path \"$CACHE_PATH\" \
      $VERBOSE_FLAG"
    echo "👉 Executing: $CMD"
    eval "$CMD 2>&1 | tee $LOG_FILE"
    ACT_EXIT_CODE=${PIPESTATUS[0]}

elif [ "$choice" == "3" ]; then
    echo "🟣 Running: Release Workflow (Container Mode)..."
    echo "⚠️  Note: Running inside Docker container."
    CMD="act push \
      -W .github/workflows/release.yml \
      -P macos-latest=-self-hosted \
      --env ACT=true \
      --secret-file \"$RUN_SECRETS\" \
      $VERBOSE_FLAG"
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
response=$(echo "$response" | tr '[:upper:]' '[:lower:]')
if [[ "$response" =~ ^(yes|y)$ ]]; then
    ./gradlew clean
    echo "✨ Cleanup complete!"
else
    echo "👌 Build files retained."
fi

exit $ACT_EXIT_CODE
