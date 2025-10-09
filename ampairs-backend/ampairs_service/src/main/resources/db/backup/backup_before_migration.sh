#!/bin/bash

# =====================================================================
# Database Backup Script - Pre-Migration Backup
# =====================================================================
#
# Purpose: Create a complete backup of the database before timezone
#          migration (LocalDateTime → Instant)
#
# Usage: ./backup_before_migration.sh [database_name]
#
# What this script backs up:
# 1. Full database dump (schema + data)
# 2. Timestamp checksums for verification
# 3. Audit report snapshot
# 4. Database configuration (timezone settings)
#
# Outputs:
# - Full database dump: backup_YYYYMMDD_HHMMSS_full.sql
# - Checksums: backup_YYYYMMDD_HHMMSS_checksums.txt
# - Audit report: backup_YYYYMMDD_HHMMSS_audit.txt
# - Config snapshot: backup_YYYYMMDD_HHMMSS_config.txt
# - Metadata: backup_YYYYMMDD_HHMMSS_metadata.json
# =====================================================================

set -e  # Exit on error
set -u  # Exit on undefined variable

# =====================================================================
# Configuration
# =====================================================================

# Default values
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_NAME="${1:-munsi_app}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# =====================================================================
# Functions
# =====================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_mysql_client() {
    if ! command -v mysql &> /dev/null; then
        log_error "mysql client not found. Please install MySQL client."
        exit 1
    fi

    if ! command -v mysqldump &> /dev/null; then
        log_error "mysqldump not found. Please install MySQL client tools."
        exit 1
    fi
}

check_database_connection() {
    log_info "Testing database connection..."

    if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1" &> /dev/null; then
        log_success "Database connection successful"
    else
        log_error "Cannot connect to database. Please check credentials."
        exit 1
    fi
}

create_backup_directory() {
    if [ ! -d "$BACKUP_DIR" ]; then
        log_info "Creating backup directory: $BACKUP_DIR"
        mkdir -p "$BACKUP_DIR"
    fi

    # Create subdirectory for this backup
    BACKUP_SUBDIR="$BACKUP_DIR/backup_$TIMESTAMP"
    mkdir -p "$BACKUP_SUBDIR"
    log_success "Backup directory created: $BACKUP_SUBDIR"
}

backup_full_database() {
    log_info "Creating full database backup..."

    DUMP_FILE="$BACKUP_SUBDIR/full_database.sql"

    mysqldump \
        -h "$DB_HOST" \
        -P "$DB_PORT" \
        -u "$DB_USER" \
        -p"$DB_PASSWORD" \
        --single-transaction \
        --routines \
        --triggers \
        --events \
        --add-drop-database \
        --databases "$DB_NAME" \
        > "$DUMP_FILE"

    # Compress the dump
    gzip "$DUMP_FILE"

    DUMP_SIZE=$(du -h "$DUMP_FILE.gz" | cut -f1)
    log_success "Database backup complete: $DUMP_FILE.gz ($DUMP_SIZE)"
}

backup_checksums() {
    log_info "Generating timestamp checksums..."

    CHECKSUM_FILE="$BACKUP_SUBDIR/checksums.txt"

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<EOF > "$CHECKSUM_FILE"
-- Timestamp Checksums for Verification
-- Generated: $(date)

SELECT 'business_types' AS table_name,
       COUNT(*) AS record_count,
       COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
       COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM business_types
UNION ALL
SELECT 'hsn_codes' AS table_name,
       COUNT(*) AS record_count,
       COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
       COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM hsn_codes
UNION ALL
SELECT 'tax_configurations' AS table_name,
       COUNT(*) AS record_count,
       COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
       COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM tax_configurations
UNION ALL
SELECT 'tax_rates' AS table_name,
       COUNT(*) AS record_count,
       COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
       COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM tax_rates
UNION ALL
SELECT 'workspace_events' AS table_name,
       COUNT(*) AS record_count,
       COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
       COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM workspace_events
UNION ALL
SELECT 'device_sessions' AS table_name,
       COUNT(*) AS record_count,
       COALESCE(SUM(UNIX_TIMESTAMP(created_at)), 0) AS created_at_checksum,
       COALESCE(SUM(UNIX_TIMESTAMP(updated_at)), 0) AS updated_at_checksum
FROM device_sessions;
EOF

    log_success "Checksums saved: $CHECKSUM_FILE"
}

backup_database_config() {
    log_info "Capturing database configuration..."

    CONFIG_FILE="$BACKUP_SUBDIR/database_config.txt"

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<EOF > "$CONFIG_FILE"
-- Database Configuration Snapshot
-- Generated: $(date)

SELECT 'MySQL Version' AS setting, VERSION() AS value
UNION ALL
SELECT 'System Timezone' AS setting, @@system_time_zone AS value
UNION ALL
SELECT 'Global Timezone' AS setting, @@global.time_zone AS value
UNION ALL
SELECT 'Session Timezone' AS setting, @@session.time_zone AS value
UNION ALL
SELECT 'Current Timestamp' AS setting, NOW() AS value
UNION ALL
SELECT 'UTC Timestamp' AS setting, UTC_TIMESTAMP() AS value;
EOF

    log_success "Configuration saved: $CONFIG_FILE"
}

run_audit_report() {
    log_info "Running full audit report..."

    AUDIT_FILE="$BACKUP_SUBDIR/audit_report.txt"
    AUDIT_SCRIPT="$(dirname "$0")/../audit/audit_timestamps.sql"

    if [ -f "$AUDIT_SCRIPT" ]; then
        mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$AUDIT_SCRIPT" > "$AUDIT_FILE"
        log_success "Audit report saved: $AUDIT_FILE"
    else
        log_warning "Audit script not found: $AUDIT_SCRIPT"
        log_warning "Skipping audit report"
    fi
}

create_metadata() {
    log_info "Creating backup metadata..."

    METADATA_FILE="$BACKUP_SUBDIR/metadata.json"

    cat > "$METADATA_FILE" <<EOF
{
  "backup_timestamp": "$TIMESTAMP",
  "backup_date": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "database_name": "$DB_NAME",
  "database_host": "$DB_HOST",
  "database_port": "$DB_PORT",
  "backup_type": "pre_timezone_migration",
  "migration_phase": "Before JPA entity migration (LocalDateTime → Instant)",
  "backup_directory": "$BACKUP_SUBDIR",
  "files": {
    "full_dump": "full_database.sql.gz",
    "checksums": "checksums.txt",
    "config": "database_config.txt",
    "audit": "audit_report.txt"
  },
  "system_info": {
    "hostname": "$(hostname)",
    "user": "$(whoami)",
    "mysql_client_version": "$(mysql --version)"
  }
}
EOF

    log_success "Metadata saved: $METADATA_FILE"
}

print_summary() {
    echo ""
    echo "========================================================================"
    echo "                    BACKUP COMPLETED SUCCESSFULLY"
    echo "========================================================================"
    echo ""
    echo "Backup Location: $BACKUP_SUBDIR"
    echo ""
    echo "Files created:"
    echo "  - full_database.sql.gz    : Complete database dump"
    echo "  - checksums.txt           : Timestamp checksums for verification"
    echo "  - database_config.txt     : MySQL configuration snapshot"
    echo "  - audit_report.txt        : Full audit report"
    echo "  - metadata.json           : Backup metadata"
    echo ""
    echo "Total Backup Size: $(du -sh "$BACKUP_SUBDIR" | cut -f1)"
    echo ""
    echo "Next Steps:"
    echo "  1. Verify backup files are present"
    echo "  2. Test restore procedure (use restore_from_backup.sh)"
    echo "  3. Proceed with migration"
    echo "  4. After migration, create another backup for comparison"
    echo ""
    echo "To restore this backup:"
    echo "  ./restore_from_backup.sh $BACKUP_SUBDIR"
    echo ""
    echo "========================================================================"
}

# =====================================================================
# Main Execution
# =====================================================================

main() {
    echo ""
    echo "========================================================================"
    echo "         DATABASE BACKUP - PRE-TIMEZONE MIGRATION"
    echo "========================================================================"
    echo ""
    echo "Database: $DB_NAME"
    echo "Host: $DB_HOST:$DB_PORT"
    echo "Timestamp: $TIMESTAMP"
    echo ""

    # Prompt for password if not set
    if [ -z "${DB_PASSWORD:-}" ]; then
        read -s -p "Enter MySQL password for user '$DB_USER': " DB_PASSWORD
        echo ""
        export DB_PASSWORD
    fi

    # Execute backup steps
    check_mysql_client
    check_database_connection
    create_backup_directory
    backup_full_database
    backup_checksums
    backup_database_config
    run_audit_report
    create_metadata
    print_summary
}

# Run main function
main
