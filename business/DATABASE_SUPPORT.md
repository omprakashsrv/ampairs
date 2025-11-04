# Business Module - Database Support

## Overview
The Business Module supports both **MySQL** and **PostgreSQL** databases through Flyway's database-specific migration feature.

## Migration Files

### Database-Specific Migrations
Flyway automatically selects the appropriate migration based on the detected database:

| Migration | MySQL Version | PostgreSQL Version |
|-----------|---------------|-------------------|
| V1.0.0 - Create table | `V1.0.0__create_businesses_table.mysql.sql` | `V1.0.0__create_businesses_table.postgresql.sql` |
| V1.0.1 - Migrate data | `V1.0.1__migrate_workspace_business_data.mysql.sql` | `V1.0.1__migrate_workspace_business_data.postgresql.sql` |

### Key Differences

#### MySQL Version Features
- `JSON` column type
- `TIMESTAMP` with `DEFAULT CURRENT_TIMESTAMP`
- `ON UPDATE CURRENT_TIMESTAMP` auto-update
- Standard indexes (no partial indexes)
- No column comments (not using `COMMENT ON`)

#### PostgreSQL Version Features
- `JSONB` column type (binary JSON, more efficient)
- `JSONB` default values: `DEFAULT '["Monday","Tuesday","Wednesday","Thursday","Friday"]'::jsonb`
- Partial indexes: `CREATE INDEX ... WHERE active = TRUE`
- Column comments: `COMMENT ON COLUMN`
- Table comments: `COMMENT ON TABLE`

## Database Column Mapping

| Entity Field (JPA) | Database Column | Type (MySQL) | Type (PostgreSQL) |
|--------------------|-----------------|--------------|-------------------|
| `uid` | `uid` | `VARCHAR(36)` | `VARCHAR(36)` |
| `ownerId` | `owner_id` | `VARCHAR(200)` | `VARCHAR(200)` |
| `taxSettings` | `tax_settings` | `JSON` | `JSONB` |
| `operatingDays` | `operating_days` | `JSON NOT NULL` | `JSONB NOT NULL DEFAULT ...` |
| `createdAt` | `created_at` | `TIMESTAMP` | `TIMESTAMP` |
| `updatedAt` | `updated_at` | `TIMESTAMP` | `TIMESTAMP` |

## Flyway Configuration

### Automatic Detection
Flyway automatically detects the database type from the JDBC URL:

```yaml
# MySQL
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/munsi_app
  flyway:
    enabled: true
    locations: classpath:db/migration  # Flyway finds .mysql.sql files

# PostgreSQL
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ampairs_db
  flyway:
    enabled: true
    locations: classpath:db/migration  # Flyway finds .postgresql.sql files
```

## Testing

### MySQL Testing
```bash
# Configure MySQL database
DB_URL=jdbc:mysql://localhost:3306/ampairs_dev
DB_USERNAME=ampairs_user
DB_PASSWORD=password

# Run application - MySQL migrations will be applied
./gradlew :ampairs_service:bootRun
```

### PostgreSQL Testing
```bash
# Configure PostgreSQL database
DB_URL=jdbc:postgresql://localhost:5432/ampairs_dev
DB_USERNAME=ampairs_user
DB_PASSWORD=password

# Run application - PostgreSQL migrations will be applied
./gradlew :ampairs_service:bootRun
```

## Migration Verification

### Check Applied Migrations
```sql
-- Both databases use the same Flyway history table
SELECT version, description, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

### Expected Output
```
version | description              | script                                              | success
--------|--------------------------|-----------------------------------------------------|--------
1.0.0   | create businesses table | V1.0.0__create_businesses_table.mysql.sql          | true
1.0.1   | migrate workspace data  | V1.0.1__migrate_workspace_business_data.mysql.sql  | true
```

## Best Practices

### ✅ DO
1. **Test both databases**: Always test migrations on both MySQL and PostgreSQL
2. **Keep versions in sync**: Both `.mysql.sql` and `.postgresql.sql` files must have the same version number
3. **Use appropriate types**: Use `JSON` for MySQL, `JSONB` for PostgreSQL
4. **Document differences**: Note any database-specific features in migration files

### ❌ DON'T
1. **Don't use PostgreSQL-only features in MySQL files** (partial indexes, `JSONB`, `COMMENT ON`)
2. **Don't use MySQL-only features in PostgreSQL files** (`ON UPDATE CURRENT_TIMESTAMP`)
3. **Don't create generic `.sql` files** - always use `.mysql.sql` or `.postgresql.sql`
4. **Don't skip testing** - both databases must pass migration

## Troubleshooting

### Issue: Flyway applies wrong migration
**Cause**: Database type not detected correctly
**Solution**: Verify JDBC URL format and database driver is in classpath

### Issue: Migration fails on one database but not the other
**Cause**: Using database-specific syntax in wrong file
**Solution**: Review migration differences table above and use appropriate syntax

### Issue: Duplicate migrations detected
**Cause**: Both `.mysql.sql` and `.postgresql.sql` have different content for same version
**Solution**: This is expected - Flyway chooses the correct one based on database type

## References
- [Flyway Database-Specific Migrations](https://documentation.red-gate.com/flyway/flyway-cli-and-api/configuration/parameters/flyway/locations)
- [MySQL JSON Type](https://dev.mysql.com/doc/refman/8.0/en/json.html)
- [PostgreSQL JSONB Type](https://www.postgresql.org/docs/current/datatype-json.html)

---

**Last Updated**: October 11, 2025
**Status**: ✅ **BOTH DATABASES SUPPORTED**
