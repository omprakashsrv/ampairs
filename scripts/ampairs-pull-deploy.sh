#!/usr/bin/env bash
#
# ampairs-pull-deploy.sh — pull-based deployment agent (UNPRIVILEGED part).
#
# Runs as the dedicated `ampairs-deploy` user on a schedule (systemd timer). It:
#   1. reads the manifest CI published to S3 (latest.json),
#   2. compares the manifest commit to what is currently deployed,
#   3. downloads + checksum-verifies the new jar (no privileges needed),
#   4. hands the verified jar to the root helper (ampairs-apply-jar) via a single,
#      tightly-scoped sudo rule, which performs the swap + restart + health gate.
#
# The agent NEVER needs inbound network: the server reaches out to S3. There is no
# SSH key in CI and no port 22 exposed to the internet.
#
# Config is loaded from /etc/ampairs/pull-deploy.env (see docs/deployment/pull-based-deploy.md).
set -euo pipefail
umask 0077

CONFIG_FILE="${AMPAIRS_PULL_CONFIG:-/etc/ampairs/pull-deploy.env}"
LOG_FILE="/var/log/ampairs/pull-deploy.log"
mkdir -p "$(dirname "$LOG_FILE")" 2>/dev/null || true
exec > >(tee -a "$LOG_FILE") 2>&1

log() { echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $*"; }

[ -f "$CONFIG_FILE" ] || { log "ERROR: $CONFIG_FILE not found"; exit 1; }
# shellcheck disable=SC1090
source "$CONFIG_FILE"

: "${ARTIFACT_S3_BUCKET:?ARTIFACT_S3_BUCKET must be set in $CONFIG_FILE}"
ARTIFACT_S3_PREFIX="${ARTIFACT_S3_PREFIX:-deploy}"
AWS_PROFILE="${AWS_PROFILE:-deploy}"
DEPLOYED_COMMIT_FILE="${DEPLOYED_COMMIT_FILE:-/opt/ampairs/production/DEPLOYED_COMMIT}"
APPLY_HELPER="${APPLY_HELPER:-/usr/local/sbin/ampairs-apply-jar}"

PREFIX_TRIMMED="${ARTIFACT_S3_PREFIX%/}"
MANIFEST_URI="s3://${ARTIFACT_S3_BUCKET}/${PREFIX_TRIMMED}/manifests/latest.json"

# Single-flight: never let two timer firings overlap.
exec 9>/run/lock/ampairs-pull-deploy.lock 2>/dev/null || exec 9>/tmp/ampairs-pull-deploy.lock
if ! flock -n 9; then
  log "Another deploy run is in progress; skipping."
  exit 0
fi

WORK_DIR="$(mktemp -d)"
cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

log "Fetching manifest ${MANIFEST_URI}"
aws --profile "$AWS_PROFILE" s3 cp "$MANIFEST_URI" "$WORK_DIR/latest.json" --only-show-errors

WANT_COMMIT=$(jq -r '.commit // empty'   "$WORK_DIR/latest.json")
JAR_KEY=$(jq -r '.jar_key // empty'      "$WORK_DIR/latest.json")
WANT_SHA256=$(jq -r '.sha256 // empty'   "$WORK_DIR/latest.json")
[ -n "$WANT_COMMIT" ] && [ -n "$JAR_KEY" ] && [ -n "$WANT_SHA256" ] \
  || { log "ERROR: manifest is missing commit/jar_key/sha256"; exit 1; }

CURRENT_COMMIT=""
[ -f "$DEPLOYED_COMMIT_FILE" ] && CURRENT_COMMIT="$(cat "$DEPLOYED_COMMIT_FILE")"

if [ "$WANT_COMMIT" = "$CURRENT_COMMIT" ]; then
  log "Already on commit ${WANT_COMMIT}; nothing to do."
  exit 0
fi

log "New version available: ${CURRENT_COMMIT:-<none>} → ${WANT_COMMIT}"
JAR_URI="s3://${ARTIFACT_S3_BUCKET}/${JAR_KEY}"
JAR_LOCAL="$WORK_DIR/ampairs-service.jar"

log "Downloading ${JAR_URI}"
aws --profile "$AWS_PROFILE" s3 cp "$JAR_URI" "$JAR_LOCAL" --only-show-errors

# Integrity gate: the artifact store is the trust boundary in a pull model, so the
# jar MUST match the checksum the manifest declares before it is ever executed.
GOT_SHA256=$(sha256sum "$JAR_LOCAL" | awk '{print $1}')
if [ "$GOT_SHA256" != "$WANT_SHA256" ]; then
  log "ERROR: checksum mismatch (want ${WANT_SHA256}, got ${GOT_SHA256}); refusing to deploy."
  exit 1
fi
if ! unzip -t "$JAR_LOCAL" >/dev/null 2>&1; then
  log "ERROR: downloaded jar is not a valid archive; refusing to deploy."
  exit 1
fi
log "Checksum OK; handing off to ${APPLY_HELPER}"

# Privileged swap + restart + health gate runs in the root helper (single sudo rule).
# The app applies Flyway migrations itself on startup (spring-boot-starter-flyway).
if sudo "$APPLY_HELPER" "$JAR_LOCAL" "$WANT_COMMIT"; then
  log "✅ Deployed commit ${WANT_COMMIT}"
else
  log "❌ Deploy of ${WANT_COMMIT} failed (helper rolled back)"
  exit 1
fi
