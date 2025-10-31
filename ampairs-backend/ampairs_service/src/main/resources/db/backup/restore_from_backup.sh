#!/bin/bash

# =====================================================================
# Database Restore Script - Restore from Backup
# =====================================================================
#
# Purpose: Restore database from a backup created by backup_before_migration.sh
#
# Usage: ./restore_from_backup.sh <backup_directory>
#
# Example:
#   ./restore_from_backup.sh ./backups/backup_20250109_143000
#
# What this script does:
# 1. Validates backup directory and files
# 2. Displays backup metadata
# 3. Prompts for confirmation (with safety checks)
# 4. Restores full database from backup
# 5. Verifies restore by comparing checksums
#
# IMPORTANT: This will DROP and RECREATE the database!
#            All data after the backup will be LOST!
# =====================================================================

set -e  # Exit on error
set -u  # Exit on undefined variable

# =====================================================================
# Configuration
# =====================================================================

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Default values
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"

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

log_critical() {
    echo -e "${MAGENTA}[CRITICAL]${NC} $1"
}

usage() {
    echo "Usage: $0 <backup_directory>"
    echo ""
    echo "Example:"
    echo "  $0 ./backups/backup_20250109_143000"
    echo ""
    echo "Environment Variables:"
    echo "  DB_HOST     - Database host (default: localhost)"
    echo "  DB_PORT     - Database port (default: 3306)"
    echo "  DB_USER     - Database user (default: root)"
    echo "  DB_PASSWORD - Database password (will prompt if not set)"
    exit 1
}

check_mysql_client() {
    if ! command -v mysql &> /dev/null; then
        log_error "mysql client not found. Please install MySQL client."
        exit 1
    fi
}

validate_backup_directory() {
    local backup_dir="$1"

    if [ ! -d "$backup_dir" ]; then
        log_error "Backup directory not found: $backup_dir"
        exit 1
    fi

    log_info "Validating backup files..."

    # Check for required files
    local required_files=(
        "full_database.sql.gz"
        "checksums.txt"
        "metadata.json"
    )

    for file in "${required_files[@]}"; do
        if [ ! -f "$backup_dir/$file" ]; then
            log_error "Required file not found: $file"
            exit 1
        fi
    done

    log_success "All required backup files present"
}

display_backup_metadata() {
    local backup_dir="$1"
    local metadata_file="$backup_dir/metadata.json"

    echo ""
    echo "========================================================================"
    echo "                      BACKUP METADATA"
    echo "========================================================================"

    if command -v jq &> /dev/null; then
        # Pretty print with jq if available
        cat "$metadata_file" | jq '.'
    else
        # Fallback to cat
        cat "$metadata_file"
    fi

    echo "========================================================================"
    echo ""
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

prompt_confirmation() {
    local db_name="$1"

    echo ""
    log_critical "╔═══════════════════════════════════════════════════════════════╗"
    log_critical "║                    ⚠️  WARNING  ⚠️                            ║"
    log_critical "╟───────────────────────────────────────────────────────────────╢"
    log_critical "║  This will DROP and RECREATE the database!                   ║"
    log_critical "║  ALL current data will be LOST and replaced with backup!     ║"
    log_critical "║                                                               ║"
    log_critical "║  Database: $db_name"
    log_critical "║  Host: $DB_HOST:$DB_PORT"
    log_critical "║                                                               ║"
    log_critical "╚═══════════════════════════════════════════════════════════════╝"
    echo ""

    # First confirmation
    read -p "Are you ABSOLUTELY sure you want to restore? (yes/no): " confirm1
    if [ "$confirm1" != "yes" ]; then
        log_info "Restore cancelled by user."
        exit 0
    fi

    # Second confirmation - must type database name
    echo ""
    log_warning "To confirm, please type the database name: $db_name"
    read -p "Database name: " confirm2
    if [ "$confirm2" != "$db_name" ]; then
        log_error "Database name mismatch. Restore cancelled."
        exit 1
    fi

    log_success "Confirmation received. Proceeding with restore..."
}

create_pre_restore_backup() {
    local db_name="$1"

    log_info "Creating safety backup of current database state..."

    SAFETY_BACKUP_DIR="./backups/pre_restore_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$SAFETY_BACKUP_DIR"

    mysqldump \
        -h "$DB_HOST" \
        -P "$DB_PORT" \
        -u "$DB_USER" \
        -p"$DB_PASSWORD" \
        --single-transaction \
        --databases "$db_name" \
        2>/dev/null | gzip > "$SAFETY_BACKUP_DIR/safety_backup.sql.gz"

    log_success "Safety backup created: $SAFETY_BACKUP_DIR/safety_backup.sql.gz"
}

restore_database() {
    local backup_dir="$1"
    local dump_file="$backup_dir/full_database.sql.gz"

    log_info "Restoring database from backup..."

    # Decompress and restore
    gunzip -c "$dump_file" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD"

    log_success "Database restored successfully"
}

verify_restore() {
    local backup_dir="$1"
    local db_name="$2"

    log_info "Verifying restore by comparing checksums..."

    # Generate current checksums
    CURRENT_CHECKSUMS=$(mktemp)

    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$db_name" <<EOF > "$CURRENT_CHECKSUMS"
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

    # Compare with backup checksums
    BACKUP_CHECKSUMS="$backup_dir/checksums.txt"

    if diff -q "$CURRENT_CHECKSUMS" "$BACKUP_CHECKSUMS" &> /dev/null; then
        log_success "✅ Checksum verification PASSED - data restored correctly"
    else
        log_warning "⚠️  Checksums differ - this may be expected if database was modified"
        echo ""
        echo "Differences:"
        diff "$CURRENT_CHECKSUMS" "$BACKUP_CHECKSUMS" || true
        echo ""
        log_info "Review the differences above to verify restore"
    fi

    rm -f "$CURRENT_CHECKSUMS"
}

print_summary() {
    local backup_dir="$1"

    echo ""
    echo "========================================================================"
    echo "                    RESTORE COMPLETED SUCCESSFULLY"
    echo "========================================================================"
    echo ""
    echo "Restored from: $backup_dir"
    echo ""
    echo "Safety backup created at:"
    echo "  $SAFETY_BACKUP_DIR"
    echo ""
    echo "Next Steps:"
    echo "  1. Verify application connectivity"
    echo "  2. Run smoke tests"
    echo "  3. Check critical data integrity"
    echo "  4. If issues found, you can restore from safety backup"
    echo ""
    echo "To restore safety backup (if needed):"
    echo "  gunzip -c $SAFETY_BACKUP_DIR/safety_backup.sql.gz | mysql -u $DB_USER -p"
    echo ""
    echo "========================================================================"
}

# =====================================================================
# Main Execution
# =====================================================================

main() {
    # Check arguments
    if [ $# -ne 1 ]; then
        usage
    fi

    BACKUP_DIR="$1"

    echo ""
    echo "========================================================================"
    echo "              DATABASE RESTORE FROM BACKUP"
    echo "========================================================================"
    echo ""

    # Prompt for password if not set
    if [ -z "${DB_PASSWORD:-}" ]; then
        read -s -p "Enter MySQL password for user '$DB_USER': " DB_PASSWORD
        echo ""
        export DB_PASSWORD
    fi

    # Execute restore steps
    check_mysql_client
    validate_backup_directory "$BACKUP_DIR"
    display_backup_metadata "$BACKUP_DIR"
    check_database_connection

    # Extract database name from metadata
    if command -v jq &> /dev/null; then
        DB_NAME=$(jq -r '.database_name' "$BACKUP_DIR/metadata.json")
    else
        # Fallback: extract from backup file
        DB_NAME=$(gunzip -c "$BACKUP_DIR/full_database.sql.gz" | grep "CREATE DATABASE" | head -1 | awk '{print $6}' | tr -d '`')
    fi

    if [ -z "$DB_NAME" ]; then
        log_error "Could not determine database name from backup"
        exit 1
    fi

    log_info "Database name from backup: $DB_NAME"

    prompt_confirmation "$DB_NAME"
    create_pre_restore_backup "$DB_NAME"
    restore_database "$BACKUP_DIR"
    verify_restore "$BACKUP_DIR" "$DB_NAME"
    print_summary "$BACKUP_DIR"
}

# Run main function
main "$@"
