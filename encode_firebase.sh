#!/bin/bash

# Define input file paths
ANDROID_JSON="androidApp/google-services.json"
IOS_PLIST="iosApp/iosApp/GoogleService-Info.plist"
WEB_JSON="google_services_web.json"

# Define output file paths
SECRETS_FILE=".secrets"

echo "------------------------------------------------"
echo "🔐 Firebase Configuration Encoder"
echo "------------------------------------------------"

# Ensure the file exists and add a newline to prevent appending to the same line
touch "$SECRETS_FILE"
echo "" >> "$SECRETS_FILE"
echo "# Auto-generated Firebase Base64 Strings ($(date))" >> "$SECRETS_FILE"

# Check for Android file
if [ -f "$ANDROID_JSON" ]; then
    echo "✅ Found Android JSON. Encoding..."
    ANDROID_BASE64=$(base64 -i "$ANDROID_JSON" | tr -d '\n')
    echo "FIREBASE_ANDROID_BASE64=$ANDROID_BASE64" >> "$SECRETS_FILE"
    echo "   -> Appended to $SECRETS_FILE"
    echo ""
else
    echo "❌ $ANDROID_JSON not found."
fi

# Check for iOS file
if [ -f "$IOS_PLIST" ]; then
    echo "✅ Found iOS Plist. Encoding..."
    IOS_BASE64=$(base64 -i "$IOS_PLIST" | tr -d '\n')
    echo "FIREBASE_IOS_BASE64=$IOS_BASE64" >> "$SECRETS_FILE"
    echo "   -> Appended to $SECRETS_FILE"
    echo ""
else
    echo "❌ $IOS_PLIST not found."
fi

# Check for Web file
if [ -f "$WEB_JSON" ]; then
    echo "✅ Found Web JSON. Encoding..."
    WEB_BASE64=$(base64 -i "$WEB_JSON" | tr -d '\n')
    echo "FIREBASE_WEB_BASE64=$WEB_BASE64" >> "$SECRETS_FILE"
    echo "   -> Appended to $SECRETS_FILE"
    echo ""
else
    echo "❌ $WEB_JSON not found."
fi

echo "------------------------------------------------"
echo "🚀 Done! Your secrets are updated in $SECRETS_FILE"
echo "💡 Remember to add $SECRETS_FILE to your .gitignore"
