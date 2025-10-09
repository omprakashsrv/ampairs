# Database Audit Scripts

This directory contains SQL scripts for auditing database state during migrations.

## Timezone Support Migration Audit

### Purpose

The `audit_timestamps.sql` script audits timestamp data before and after the timezone support migration (LocalDateTime → Instant).

### Key Findings

**Important**: MySQL `TIMESTAMP` columns already store data in UTC internally. No database migration is needed!

- JPA migration (LocalDateTime → Instant) is transparent to the database layer
- With `serverTimezone=UTC` in JDBC URL, no timezone conversion occurs
- API responses will serialize as ISO-8601 with Z suffix automatically

### Running the Audit Script

#### Prerequisites

1. Database connection with appropriate credentials
2. MySQL client (mysql command-line tool or MySQL Workbench)

#### Execute the Script

```bash
# Using mysql command-line
mysql -u [username] -p [database_name] < audit_timestamps.sql > audit_output_$(date +%Y%m%d_%H%M%S).txt

# Or with explicit connection
mysql -h localhost -u root -p munsi_app < audit_timestamps.sql > audit_before_migration.txt
```

#### Using MySQL Workbench

1. Open MySQL Workbench
2. Connect to your database
3. Open `audit_timestamps.sql`
4. Execute the entire script (Ctrl+Shift+Enter)
5. Review results in the Output panel

### Audit Report Sections

The audit script generates 8 comprehensive sections:

#### 1. Database Configuration
- Current timezone settings (system, global, session)
- Verifies MySQL timezone configuration

#### 2. Timestamp Columns Inventory
- Lists all tables with TIMESTAMP/DATETIME columns
- Shows column types, nullability, defaults
- Counts timestamp columns per table

#### 3. Data Range Analysis
- Earliest and latest timestamps per table
- Helps identify data distribution
- Useful for capacity planning

#### 4. Data Quality Checks
- NULL value detection in timestamp columns
- Future timestamp detection (data quality issues)
- Logical errors (updated_at < created_at)

#### 5. Sample Data Verification
- First 5 records from each table
- Visual verification of timestamp values
- Compare before/after migration

#### 6. Timezone Assumptions Documentation
- Documents MySQL TIMESTAMP behavior
- Migration impact analysis
- Post-migration verification steps

#### 7. Pre-Migration Backup Checksums
- Creates checksums for all timestamp data
- Used to verify data integrity after migration
- Compare checksums before/after

#### 8. Summary and Recommendations
- Total tables and columns affected
- Step-by-step migration checklist
- Next steps guidance

### Migration Workflow

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Run audit script BEFORE migration                       │
│    → Save output as audit_before_migration.txt             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Perform JPA migration (LocalDateTime → Instant)         │
│    → Update entity classes                                 │
│    → Update service layer                                  │
│    → Update controller responses                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Run integration tests                                    │
│    → Verify timestamp serialization                        │
│    → Check API response format                             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Run audit script AFTER migration                        │
│    → Save output as audit_after_migration.txt              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Compare outputs                                          │
│    → Verify checksums match (Section 7)                    │
│    → Confirm no data corruption                            │
│    → Check for timezone shifts                             │
└─────────────────────────────────────────────────────────────┘
```

### Verification Checklist

After running both pre- and post-migration audits:

- [ ] Checksums match for all tables (Section 7)
- [ ] No new NULL values introduced (Section 4)
- [ ] No future timestamps detected (Section 4)
- [ ] No logical errors (updated_at < created_at) (Section 4)
- [ ] Sample data shows same values (Section 5)
- [ ] API responses use ISO-8601 format with Z suffix
- [ ] Client applications display correct local time

### Expected Results

**No database changes expected!**

Because MySQL `TIMESTAMP` columns already store UTC:
- Pre-migration checksums = Post-migration checksums
- Timestamp values should be identical
- Only difference is JPA mapping layer (transparent)

### Troubleshooting

#### Issue: Checksums don't match

**Cause**: Data was inserted/updated between audit runs

**Solution**:
1. Stop application
2. Run both audits again
3. Compare checksums

#### Issue: Timezone shifts detected

**Cause**: `serverTimezone` not set correctly in JDBC URL

**Solution**:
1. Verify `serverTimezone=UTC` in application.yml
2. Restart application
3. Check @@session.time_zone in audit output

#### Issue: NULL values introduced

**Cause**: Entity creation logic not setting timestamps

**Solution**:
1. Check `@PrePersist` and `@PreUpdate` annotations
2. Verify `BaseDomain.prePersist()` is called
3. Add explicit `Instant.now()` if needed

### Additional Resources

- [MySQL TIMESTAMP Documentation](https://dev.mysql.com/doc/refman/8.0/en/datetime.html)
- [Java Instant API](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html)
- [ISO-8601 Format Specification](https://en.wikipedia.org/wiki/ISO_8601)
- [Project Timezone Support Plan](/specs/002-timezone-support/plan.md)

### Contact

For questions or issues with this audit script:
1. Check the main project documentation in `/specs/002-timezone-support/`
2. Review CLAUDE.md for architectural patterns
3. Consult the development team
