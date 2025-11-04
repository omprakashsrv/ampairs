#!/bin/bash
# Helper script to copy Firebase service account key to clipboard
# Usage: ./.github/scripts/copy-firebase-secret.sh

set -e

FIREBASE_KEY_PATH="ampairs_service/keys/ampairs-firebase-adminsdk.json"

echo "=================================================="
echo "Firebase Service Account Key - GitHub Secret Setup"
echo "=================================================="
echo ""

# Check if file exists
if [ ! -f "$FIREBASE_KEY_PATH" ]; then
  echo "❌ ERROR: Firebase key file not found at:"
  echo "   $FIREBASE_KEY_PATH"
  echo ""
  echo "Please ensure the file exists before running this script."
  exit 1
fi

# Validate JSON
if ! jq empty "$FIREBASE_KEY_PATH" 2>/dev/null; then
  echo "❌ ERROR: Firebase key file is not valid JSON"
  exit 1
fi

echo "✅ Firebase key file found and validated"
echo ""
echo "File location: $FIREBASE_KEY_PATH"
echo "File size: $(wc -c < "$FIREBASE_KEY_PATH") bytes"
echo ""


# Try to copy to clipboard
if command -v pbcopy &> /dev/null; then
  # macOS
  cat "$FIREBASE_KEY_PATH" | pbcopy
  echo "✅ Firebase key copied to clipboard (macOS)"
elif command -v xclip &> /dev/null; then
  # Linux with xclip
  cat "$FIREBASE_KEY_PATH" | xclip -selection clipboard
  echo "✅ Firebase key copied to clipboard (Linux)"
elif command -v xsel &> /dev/null; then
  # Linux with xsel
  cat "$FIREBASE_KEY_PATH" | xsel --clipboard
  echo "✅ Firebase key copied to clipboard (Linux)"
else
  echo "⚠️  Could not copy to clipboard automatically"
  echo ""
  echo "Please manually copy the content of:"
  echo "   $FIREBASE_KEY_PATH"
  echo ""
  echo "You can use: cat $FIREBASE_KEY_PATH"
fi

echo ""
echo "=================================================="
echo "Next Steps:"
echo "=================================================="
echo ""
echo "1. Go to your GitHub repository:"
echo "   https://github.com/YOUR_ORG/ampairs/settings/secrets/actions"
echo ""
echo "2. Click 'New repository secret'"
echo ""
echo "3. Set the following:"
echo "   Name:  FIREBASE_SERVICE_ACCOUNT_KEY"
echo "   Value: Paste the JSON content (already in clipboard)"
echo ""
echo "4. Click 'Add secret'"
echo ""
echo "5. Push your changes and re-run the workflow"
echo ""
echo "=================================================="
