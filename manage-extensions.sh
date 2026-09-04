#!/bin/bash

# Navigate to the project root
cd "$(dirname "$0")"

echo "🛠️ OmniHub Extension Management Tool"

# 1. Check for installed extensions
if [ "$1" == "list" ]; then
    firebase ext:list
    exit 0
fi

# 2. Update and Deploy flow
INSTANCE_ID=$1
VERSION_OR_SOURCE=$2
PROJECT_ID=$3

if [ -z "$INSTANCE_ID" ] || [ -z "$VERSION_OR_SOURCE" ]; then
    echo "💡 Usage: ./manage-extensions.sh <instance-id> <version-or-source> [project-id]"
    echo "Example (Registry): ./manage-extensions.sh omnifeed-auth-custom-service lackstudio/omnifeed-auth-custom-service@0.13.0-alpha.6 lackstudio-omnihub-dev"
    echo "Example (Local):    ./manage-extensions.sh omnifeed-auth-custom-service ../omnifeed-kmp/firebase-extension lackstudio-omnihub-dev"
    exit 1
fi

# Construct project flag if provided
PROJECT_FLAG=""
if [ ! -z "$PROJECT_ID" ]; then
    PROJECT_FLAG="--project $PROJECT_ID"
fi

echo "🔄 Step 1: Updating local manifest for $INSTANCE_ID..."
firebase ext:update "$INSTANCE_ID" "$VERSION_OR_SOURCE" $PROJECT_FLAG

if [ $? -eq 0 ]; then
    echo "🚀 Step 2: Deploying changes to Firebase Console..."
    firebase deploy --only extensions $PROJECT_FLAG

    if [ $? -eq 0 ]; then
        echo "✅ Extension $INSTANCE_ID is now live!"
    else
        echo "❌ Deployment failed."
    fi
else
    echo "❌ Local update failed. Manifest not changed."
fi
