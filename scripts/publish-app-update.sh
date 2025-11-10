#!/bin/bash

###############################################################################
# Publish Desktop App Update
#
# This script uploads an app binary to S3 and registers it in the database.
# Can be used manually or in CI/CD pipelines.
#
# Usage:
#   ./scripts/publish-app-update.sh <file> <version> <platform> [options]
#
# Examples:
#   ./scripts/publish-app-update.sh Ampairs-1.0.0.10.dmg 1.0.0.10 MACOS
#   ./scripts/publish-app-update.sh Ampairs.msi 1.0.0.11 WINDOWS --mandatory
#
# Environment Variables:
#   AWS_REGION                   - AWS region (default: ap-south-1)
#   S3_BUCKET                    - S3 bucket name (default: ampairs-app-updates)
#   API_BASE_URL                 - API base URL (default: https://api.ampairs.in)
#   AMPAIRS_API_KEY              - API Key for authentication (required)
#   AWS_ACCESS_KEY_ID            - AWS access key (required)
#   AWS_SECRET_ACCESS_KEY        - AWS secret key (required)
###############################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION="${AWS_REGION:-ap-south-1}"
S3_BUCKET="${S3_BUCKET:-ampairs-app-updates}"
S3_PREFIX="${S3_PREFIX:-updates}"
API_BASE_URL="${API_BASE_URL:-https://api.ampairs.in}"

# Functions
log_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

log_success() {
    echo -e "${GREEN}✓${NC} $1"
}

log_error() {
    echo -e "${RED}✗${NC} $1" >&2
}

log_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

usage() {
    cat <<EOF
Usage: $0 <file> <version> <platform> [options]

Arguments:
    file        Path to the app binary (dmg, msi, deb, etc.)
    version     Version string (e.g., 1.0.0.10)
    platform    Platform: MACOS, WINDOWS, or LINUX

Options:
    --mandatory              Mark this update as mandatory
    --release-notes <file>   Path to release notes file (markdown)
    --min-version <version>  Minimum supported version
    --dry-run               Validate but don't upload/register

Environment Variables:
    AMPAIRS_API_KEY         API Key for authentication (required)
    AWS_ACCESS_KEY_ID       AWS access key (required)
    AWS_SECRET_ACCESS_KEY   AWS secret key (required)
    S3_BUCKET               S3 bucket name (default: ampairs-app-updates)
    API_BASE_URL            API URL (default: https://api.ampairs.in)

Examples:
    $0 Ampairs-1.0.0.10.dmg 1.0.0.10 MACOS
    $0 Ampairs.msi 1.0.0.11 WINDOWS --mandatory --min-version 1.0.0.5

EOF
    exit 1
}

# Parse arguments
if [ $# -lt 3 ]; then
    usage
fi

FILE_PATH="$1"
VERSION="$2"
PLATFORM="$3"
shift 3

IS_MANDATORY=false
RELEASE_NOTES=""
MIN_VERSION=""
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --mandatory)
            IS_MANDATORY=true
            shift
            ;;
        --release-notes)
            if [ -f "$2" ]; then
                RELEASE_NOTES=$(cat "$2")
            else
                log_error "Release notes file not found: $2"
                exit 1
            fi
            shift 2
            ;;
        --min-version)
            MIN_VERSION="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Validate inputs
log_info "Validating inputs..."

if [ ! -f "$FILE_PATH" ]; then
    log_error "File not found: $FILE_PATH"
    exit 1
fi

if [ -z "$AMPAIRS_API_KEY" ]; then
    log_error "AMPAIRS_API_KEY environment variable is required"
    log_error "See API_KEY_AUTHENTICATION.md for setup instructions"
    exit 1
fi

if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
    log_error "AWS credentials required: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY"
    exit 1
fi

# Validate platform
if [[ ! "$PLATFORM" =~ ^(MACOS|WINDOWS|LINUX)$ ]]; then
    log_error "Invalid platform: $PLATFORM (must be MACOS, WINDOWS, or LINUX)"
    exit 1
fi

# Extract version code (last number)
VERSION_CODE="${VERSION##*.}"
if ! [[ "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
    log_error "Invalid version format: $VERSION (expected format: x.x.x.N where N is a number)"
    exit 1
fi

log_success "Inputs validated"

# Get file info
log_info "Analyzing file..."

FILENAME=$(basename "$FILE_PATH")
FILE_EXTENSION="${FILENAME##*.}"

# Calculate checksum
log_info "Calculating SHA-256 checksum..."
if command -v sha256sum &> /dev/null; then
    CHECKSUM=$(sha256sum "$FILE_PATH" | awk '{print $1}')
elif command -v shasum &> /dev/null; then
    CHECKSUM=$(shasum -a 256 "$FILE_PATH" | awk '{print $1}')
else
    log_error "Neither sha256sum nor shasum found"
    exit 1
fi
log_success "Checksum: $CHECKSUM"

# Calculate file size in MB
if [[ "$OSTYPE" == "darwin"* ]]; then
    SIZE_BYTES=$(stat -f%z "$FILE_PATH")
else
    SIZE_BYTES=$(stat -c%s "$FILE_PATH")
fi
SIZE_MB=$(echo "scale=2; $SIZE_BYTES / 1024 / 1024" | bc)
log_success "File size: $SIZE_MB MB"

# Generate S3 key
PLATFORM_LOWER=$(echo "$PLATFORM" | tr '[:upper:]' '[:lower:]')
S3_KEY="${S3_PREFIX}/${PLATFORM_LOWER}-${VERSION}.${FILE_EXTENSION}"
log_info "S3 key: $S3_KEY"

# Default release notes if not provided
if [ -z "$RELEASE_NOTES" ]; then
    RELEASE_NOTES="Release $VERSION for $PLATFORM"
fi

# Display summary
echo ""
log_info "========================================="
log_info "Release Summary"
log_info "========================================="
echo "File:           $FILENAME"
echo "Version:        $VERSION (code: $VERSION_CODE)"
echo "Platform:       $PLATFORM"
echo "Mandatory:      $IS_MANDATORY"
echo "Size:           $SIZE_MB MB"
echo "Checksum:       $CHECKSUM"
echo "S3 Bucket:      s3://$S3_BUCKET/$S3_KEY"
echo "API:            $API_BASE_URL"
if [ -n "$MIN_VERSION" ]; then
    echo "Min Version:    $MIN_VERSION"
fi
echo ""

if [ "$DRY_RUN" = true ]; then
    log_warning "DRY RUN MODE - No changes will be made"
    exit 0
fi

# Confirm before proceeding (unless in CI)
if [ -z "${CI:-}" ]; then
    read -p "Proceed with upload and registration? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        log_warning "Cancelled by user"
        exit 0
    fi
fi

# Upload to S3
log_info "Uploading to S3..."
aws s3 cp "$FILE_PATH" "s3://$S3_BUCKET/$S3_KEY" \
    --region "$AWS_REGION" \
    --metadata "version=$VERSION,platform=$PLATFORM,checksum=$CHECKSUM" \
    --no-progress

log_success "Uploaded to S3: s3://$S3_BUCKET/$S3_KEY"

# Register in database
log_info "Registering version in database..."

# Prepare JSON payload
JSON_PAYLOAD=$(cat <<EOF
{
  "version": "$VERSION",
  "version_code": $VERSION_CODE,
  "platform": "$PLATFORM",
  "is_mandatory": $IS_MANDATORY,
  "s3_key": "$S3_KEY",
  "filename": "$FILENAME",
  "file_size_mb": $SIZE_MB,
  "checksum": "$CHECKSUM",
  "release_notes": $(echo "$RELEASE_NOTES" | jq -Rs .),
  "release_date": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")"$(if [ -n "$MIN_VERSION" ]; then echo ",
  \"min_supported_version\": \"$MIN_VERSION\""; fi)
}
EOF
)

# Call API using API Key authentication
HTTP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE_URL/api/v1/app-updates" \
    -H "X-API-Key: $AMPAIRS_API_KEY" \
    -H "Content-Type: application/json" \
    -d "$JSON_PAYLOAD")

HTTP_BODY=$(echo "$HTTP_RESPONSE" | sed '$d')
HTTP_CODE=$(echo "$HTTP_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 201 ]; then
    log_success "Version registered in database"

    # Extract UID from response
    UID=$(echo "$HTTP_BODY" | jq -r '.data.uid // empty')
    if [ -n "$UID" ]; then
        log_success "Version UID: $UID"
    fi
else
    log_error "Failed to register version (HTTP $HTTP_CODE)"
    echo "$HTTP_BODY" | jq . 2>/dev/null || echo "$HTTP_BODY"
    exit 1
fi

# Success!
echo ""
log_success "========================================="
log_success "Release published successfully! 🎉"
log_success "========================================="
echo ""
echo "Version $VERSION for $PLATFORM is now available."
echo "Users will be notified through the in-app update checker."
echo ""
echo "Download URL: $API_BASE_URL/api/v1/app-updates/download/$UID"
echo ""
