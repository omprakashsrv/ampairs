#!/usr/bin/env bash
#
# ampairs-apply-jar.sh — pull-based deployment agent (PRIVILEGED part).
#
# Installed as /usr/local/sbin/ampairs-apply-jar, owned root:root mode 0755.
# The ONLY command the ampairs-deploy user may run via sudo (see ampairs-deploy-sudoers).
# Keeping the privileged surface to this one fixed, argument-validated script means a
# compromise of the unprivileged agent cannot run arbitrary commands as root.
#
# Usage: ampairs-apply-jar <verified-jar-path> <commit-sha>
#
# Performs an atomic-ish swap with rollback:
#   stop → back up current jar → install new jar → write commit marker → start →
#   health gate; on failure, restore the previous jar + marker and restart.
set -euo pipefail

DEPLOY_DIR="/opt/ampairs/production"
JAR_DEST="${DEPLOY_DIR}/ampairs-service.jar"
JAR_PREV="${DEPLOY_DIR}/ampairs-service.previous.jar"
COMMIT_FILE="${DEPLOY_DIR}/DEPLOYED_COMMIT"
COMMIT_PREV="${DEPLOY_DIR}/DEPLOYED_COMMIT.previous"
SERVICE="ampairs-production"
SERVICE_USER="ampairs"
HEALTH_URL="http://localhost:8080/api/actuator/health"

NEW_JAR="${1:?usage: ampairs-apply-jar <jar> <commit>}"
NEW_COMMIT="${2:?usage: ampairs-apply-jar <jar> <commit>}"

log() { echo "[apply-jar] $*"; }

[ -f "$NEW_JAR" ] || { log "ERROR: jar not found: $NEW_JAR"; exit 1; }
# Re-verify the archive here too — this script runs as root and must not trust its caller.
unzip -t "$NEW_JAR" >/dev/null 2>&1 || { log "ERROR: not a valid jar: $NEW_JAR"; exit 1; }

mkdir -p "$DEPLOY_DIR"

log "Stopping ${SERVICE}"
systemctl stop "$SERVICE" || true

# Back up the currently deployed jar + marker for rollback.
if [ -f "$JAR_DEST" ]; then cp -f "$JAR_DEST" "$JAR_PREV"; fi
if [ -f "$COMMIT_FILE" ]; then cp -f "$COMMIT_FILE" "$COMMIT_PREV"; fi

log "Installing new jar (commit ${NEW_COMMIT})"
install -o "$SERVICE_USER" -g "$SERVICE_USER" -m 0640 "$NEW_JAR" "$JAR_DEST"
printf '%s' "$NEW_COMMIT" > "$COMMIT_FILE"
chown "$SERVICE_USER:$SERVICE_USER" "$COMMIT_FILE"

log "Starting ${SERVICE}"
systemctl start "$SERVICE"

# Health gate: the app applies Flyway migrations on startup, so allow generous time.
log "Waiting for health at ${HEALTH_URL}"
for i in $(seq 1 18); do
  if curl -fsS --max-time 5 "$HEALTH_URL" >/dev/null 2>&1; then
    log "✅ Healthy on commit ${NEW_COMMIT}"
    exit 0
  fi
  sleep 10
done

# Rollback.
log "❌ New version unhealthy; rolling back"
if [ -f "$JAR_PREV" ]; then
  install -o "$SERVICE_USER" -g "$SERVICE_USER" -m 0640 "$JAR_PREV" "$JAR_DEST"
  [ -f "$COMMIT_PREV" ] && cp -f "$COMMIT_PREV" "$COMMIT_FILE" && chown "$SERVICE_USER:$SERVICE_USER" "$COMMIT_FILE"
  systemctl restart "$SERVICE" || true
  log "Rolled back to previous jar"
else
  log "No previous jar to roll back to"
fi
exit 1
