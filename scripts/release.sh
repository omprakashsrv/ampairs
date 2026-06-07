#!/usr/bin/env bash
set -euo pipefail

# ---------------------------------------------------------------------------
# release.sh  — bump version, commit, tag, push
#
# Usage:
#   ./scripts/release.sh <version> [message]
#
# Examples:
#   ./scripts/release.sh 1.0.9
#   ./scripts/release.sh 1.0.9 "Add master tax code search and product lazy-load fixes"
# ---------------------------------------------------------------------------

VERSION="${1:-}"
MESSAGE="${2:-}"

# ── Validate inputs ─────────────────────────────────────────────────────────

if [[ -z "$VERSION" ]]; then
  echo "Error: version is required"
  echo "Usage: $0 <version> [message]"
  exit 1
fi

if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: version must be semver (e.g. 1.0.9), got: $VERSION"
  exit 1
fi

TAG="v${VERSION}"

# Prompt for message if not provided
if [[ -z "$MESSAGE" ]]; then
  echo "Release message (leave blank for default):"
  read -r MESSAGE
  if [[ -z "$MESSAGE" ]]; then
    MESSAGE="Release ${TAG}"
  fi
fi

# ── Pre-flight checks ────────────────────────────────────────────────────────

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

# Abort if tag already exists
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "Error: tag $TAG already exists"
  exit 1
fi

# Abort if working tree is dirty
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Error: working tree has uncommitted changes — commit or stash first"
  git status --short
  exit 1
fi

# ── Bump version in build.gradle.kts ────────────────────────────────────────

BUILD_FILE="$ROOT_DIR/build.gradle.kts"
CURRENT_VERSION=$(grep -m1 '^version = ' "$BUILD_FILE" | sed 's/version = "\(.*\)"/\1/')

if [[ "$CURRENT_VERSION" == "$VERSION" ]]; then
  echo "Version is already $VERSION — skipping version bump"
else
  echo "Bumping $CURRENT_VERSION → $VERSION"
  sed -i.bak "s/version = \"${CURRENT_VERSION}\"/version = \"${VERSION}\"/g" "$BUILD_FILE"
  rm -f "$BUILD_FILE.bak"

  git add build.gradle.kts
  git commit -m "chore(release): bump version to ${VERSION}

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>"
fi

# ── Create annotated tag ─────────────────────────────────────────────────────

git tag -a "$TAG" -m "$MESSAGE"
echo "Created tag $TAG"

# ── Push ─────────────────────────────────────────────────────────────────────

git push origin main
git push origin "$TAG"

echo ""
echo "✅  Released $TAG"
echo "    $MESSAGE"
