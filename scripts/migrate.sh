#!/usr/bin/env bash
# Run Flyway migrations against a local development database.
#
# In production, Flyway runs automatically on Spring Boot startup —
# SQL files are packaged inside the fat JAR and discovered via
# classpath:db/migration/{vendor}.
#
# Usage:
#   ./scripts/migrate.sh                  # migrate (default)
#   ./scripts/migrate.sh info             # show migration status
#   ./scripts/migrate.sh validate
#   ./scripts/migrate.sh repair
#   ./scripts/migrate.sh baseline
#
# Override connection with env vars:
#   DB_URL=jdbc:postgresql://host:5432/db \
#     DB_USERNAME=user DB_PASSWORD=pass \
#     ./scripts/migrate.sh

set -euo pipefail

COMMAND="${1:-migrate}"

export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/springdb}"
export DB_USERNAME="${DB_USERNAME:-springuser}"
export DB_PASSWORD="${DB_PASSWORD:-springpass}"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "DB      : $DB_URL"
echo "User    : $DB_USERNAME"
echo "Command : $COMMAND"
echo ""

case "$COMMAND" in
  info)     TASK=":ampairs_service:dbInfo" ;;
  validate) TASK=":ampairs_service:dbValidate" ;;
  repair)   TASK=":ampairs_service:dbRepair" ;;
  *)        TASK=":ampairs_service:dbMigrate" ;;
esac

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" "$TASK" --no-daemon
