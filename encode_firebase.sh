#!/bin/bash

# -----------------------------------------------------------------------------
# 🔐 Firebase Configuration Encoder
# -----------------------------------------------------------------------------
# This script encodes your Firebase configuration files into Base64.
#
# MANDATORY:
# 1. Create a file named '.firebase_paths' in the project root.
# 2. Define the following variables in that file:
#    ANDROID_JSON="path/to/google-services.json"
#    IOS_PLIST="path/to/GoogleService-Info.plist"
#    WEB_JSON="path/to/google_services_web.json"
# -----------------------------------------------------------------------------

SECRETS_FILE=".secrets"
CONFIG_FILE=".firebase_paths"

echo "------------------------------------------------"
echo "🔐 Firebase Configuration Encoder"
echo "------------------------------------------------"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "❌ ERROR: $CONFIG_FILE not found!"
    echo "Please create $CONFIG_FILE based on .firebase_paths.example"
    exit 1
fi

# Load paths
source "$CONFIG_FILE"

# Function to validate and encode
encode_file() {
    local label=$1
    local file_path=$2
    local secret_key=$3

    if [ -z "$file_path" ]; then
        echo "❌ ERROR: Path for $label is not defined in $CONFIG_FILE"
        exit 1
    fi

    if [ -f "$file_path" ]; then
        echo "✅ Found $label ($file_path). Encoding..."
        local base64_str=$(base64 -i "$file_path" | tr -d '\n')
        echo "$secret_key=$base64_str" >> "$SECRETS_FILE"
        echo "   -> Appended to $SECRETS_FILE"
    else
        echo "❌ ERROR: $label file not found at: $file_path"
        exit 1
    fi
    echo ""
}

# Ensure the secrets file exists and add a header
touch "$SECRETS_FILE"
echo "" >> "$SECRETS_FILE"
echo "# Auto-generated Firebase Base64 Strings ($(date))" >> "$SECRETS_FILE"

# Process configurations strictly
encode_file "Android JSON" "$ANDROID_JSON" "FIREBASE_ANDROID_BASE64"
encode_file "iOS Plist" "$IOS_PLIST" "FIREBASE_IOS_BASE64"
encode_file "Web JSON" "$WEB_JSON" "FIREBASE_WEB_BASE64"

echo "------------------------------------------------"
echo "🚀 Done! Your secrets are updated in $SECRETS_FILE"
