# Database Backup and Restore Scripts

This directory contains scripts for backing up and restoring the database before/after the timezone migration.

## Overview

### Why Backup?

Although the timezone migration (LocalDateTime → Instant) does **NOT** change the database schema or data, we create backups as a safety measure:

- **No database migration needed**: MySQL TIMESTAMP already stores UTC
- **Safety first**: Backups provide rollback capability
- **Verification**: Compare checksums before/after migration
- **Compliance**: Document database state at migration time

## Scripts

### 1. backup_before_migration.sh

Creates a comprehensive backup before migration.

**What it backs up:**
- Full database dump (schema + data)
- Timestamp checksums for verification
- Database configuration (timezone settings)
- Full audit report
- Metadata (JSON format)

**Usage:**
```bash
# Set environment variables (optional)
export DB_HOST=localhost
export DB_PORT=3306
export DB_USER=root
export DB_PASSWORD=your_password
export BACKUP_DIR=./backups

# Run backup
./backup_before_migration.sh munsi_app
```

**Interactive mode:**
```bash
# Script will prompt for password
./backup_before_migration.sh munsi_app
Enter MySQL password for user 'root': ****
```

**Output structure:**
```
backups/
└── backup_20250109_143000/
    ├── full_database.sql.gz      # Complete database dump (compressed)
    ├── checksums.txt             # Timestamp checksums
    ├── database_config.txt       # MySQL config snapshot
    ├── audit_report.txt          # Full audit report
    └── metadata.json             # Backup metadata
```

### 2. restore_from_backup.sh

Restores database from a backup created by backup_before_migration.sh.

**DANGER**: This will DROP and RECREATE the database! All current data will be lost!

**Safety features:**
- Two-step confirmation required
- Must type database name to confirm
- Creates safety backup before restore
- Verifies restore with checksums

**Usage:**
```bash
# Restore from specific backup
./restore_from_backup.sh ./backups/backup_20250109_143000
```

**Confirmation process:**
```
⚠️  WARNING: This will DROP and RECREATE the database!
ALL current data will be LOST and replaced with backup!

Are you ABSOLUTELY sure you want to restore? (yes/no): yes

To confirm, please type the database name: munsi_app
Database name: munsi_app

✅ Confirmation received. Proceeding with restore...
```

## Backup Workflow

### Before Migration

```bash
# Step 1: Create pre-migration backup
./backup_before_migration.sh munsi_app

# Step 2: Verify backup files
ls -lh backups/backup_20250109_143000/

# Step 3: Test restore (on dev environment)
./restore_from_backup.sh ./backups/backup_20250109_143000
```

### After Migration

```bash
# Step 1: Create post-migration backup
./backup_before_migration.sh munsi_app

# Step 2: Compare checksums
diff backups/backup_BEFORE/checksums.txt backups/backup_AFTER/checksums.txt

# Expected: No differences (data unchanged)
```

### If Rollback Needed

```bash
# Emergency rollback procedure
./restore_from_backup.sh ./backups/backup_BEFORE_MIGRATION

# Verify application connectivity
curl http://localhost:8080/actuator/health

# Revert code changes
git revert <migration-commit>
./gradlew clean build
./gradlew :ampairs_service:bootRun
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | Database server hostname |
| DB_PORT | 3306 | Database server port |
| DB_USER | root | Database username |
| DB_PASSWORD | (prompt) | Database password |
| BACKUP_DIR | ./backups | Directory for backup storage |

## Backup Best Practices

### 1. Pre-Migration Checklist

- [ ] Stop application to ensure consistent backup
- [ ] Verify database connection (`mysql -u root -p -e "SELECT 1"`)
- [ ] Check available disk space (`df -h`)
- [ ] Set proper environment variables
- [ ] Run backup script
- [ ] Verify all files created
- [ ] Test restore on dev environment

### 2. Storage Recommendations

| Environment | Retention | Location |
|-------------|-----------|----------|
| Development | 7 days | Local disk |
| Staging | 30 days | Local + remote |
| Production | 90 days | Remote (S3, etc.) |

### 3. Security

**Protect backup files:**
```bash
# Set proper permissions
chmod 600 backups/backup_*/full_database.sql.gz
chmod 600 backups/backup_*/checksums.txt

# Encrypt backups (optional)
gpg --encrypt --recipient your@email.com backups/backup_*/full_database.sql.gz
```

**Secure credentials:**
```bash
# Use .my.cnf for password
cat > ~/.my.cnf <<EOF
[client]
user=root
password=your_password
host=localhost
EOF

chmod 600 ~/.my.cnf

# Run backup without password prompt
./backup_before_migration.sh munsi_app
```

## Troubleshooting

### Issue: "mysqldump: command not found"

**Solution:**
```bash
# macOS
brew install mysql-client

# Ubuntu/Debian
sudo apt-get install mysql-client

# RHEL/CentOS
sudo yum install mysql
```

### Issue: "Access denied for user"

**Solution:**
```bash
# Verify credentials
mysql -h localhost -u root -p -e "SELECT 1"

# Check user permissions
mysql -u root -p -e "SHOW GRANTS FOR 'root'@'localhost'"

# Grant necessary permissions
GRANT SELECT, LOCK TABLES, SHOW VIEW, RELOAD, REPLICATION CLIENT ON *.* TO 'root'@'localhost';
```

### Issue: "Disk space full"

**Solution:**
```bash
# Check available space
df -h

# Compress old backups
find backups/ -name "*.sql" -exec gzip {} \;

# Delete old backups (older than 30 days)
find backups/ -type d -name "backup_*" -mtime +30 -exec rm -rf {} \;
```

### Issue: "Checksums don't match after restore"

**Possible causes:**
1. Data was modified between backup and restore
2. Timezone differences in UNIX_TIMESTAMP calculations
3. Different MySQL versions

**Solution:**
```bash
# Manually verify sample data
mysql -u root -p munsi_app <<EOF
SELECT uid, created_at, updated_at FROM business_types LIMIT 5;
EOF

# Compare with backup audit report
cat backups/backup_BEFORE/audit_report.txt | grep "business_types - Sample"
```

## Automated Backups (Optional)

### Using Cron

```bash
# Add to crontab
crontab -e

# Backup daily at 2 AM
0 2 * * * /path/to/backup_before_migration.sh munsi_app >> /var/log/db_backup.log 2>&1

# Cleanup old backups weekly
0 3 * * 0 find /path/to/backups -type d -name "backup_*" -mtime +30 -delete
```

### Using systemd Timer (Linux)

```bash
# Create service file
sudo nano /etc/systemd/system/db-backup.service

[Unit]
Description=Database Backup Service

[Service]
Type=oneshot
ExecStart=/path/to/backup_before_migration.sh munsi_app
User=your_user

# Create timer file
sudo nano /etc/systemd/system/db-backup.timer

[Unit]
Description=Database Backup Timer

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target

# Enable and start
sudo systemctl enable db-backup.timer
sudo systemctl start db-backup.timer
```

## Monitoring

### Check Backup Success

```bash
# View latest backup
ls -lt backups/ | head -5

# Check backup integrity
gunzip -t backups/backup_LATEST/full_database.sql.gz
echo $?  # Should be 0

# Verify backup size
du -sh backups/backup_*/full_database.sql.gz
```

### Alert on Failure

```bash
# Add to backup script
if [ $? -ne 0 ]; then
    echo "Backup failed!" | mail -s "DB Backup Failure" admin@example.com
fi
```

## Additional Resources

- [MySQL Backup Documentation](https://dev.mysql.com/doc/refman/8.0/en/backup-methods.html)
- [mysqldump Reference](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html)
- [MySQL Point-in-Time Recovery](https://dev.mysql.com/doc/refman/8.0/en/point-in-time-recovery.html)
- [Project Timezone Migration Plan](/specs/002-timezone-support/plan.md)
- [Database Audit Scripts](../audit/README.md)

## Support

For issues with backup/restore scripts:
1. Check script logs and error messages
2. Review troubleshooting section above
3. Verify MySQL client tools are installed
4. Check database connection and permissions
5. Consult the development team
