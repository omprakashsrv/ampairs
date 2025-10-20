# Migration Script Templates

**Feature**: Database Schema Migration with Flyway
**Date**: 2025-10-12
**Purpose**: Template files for creating Flyway migration scripts

## Files

### migration-template.sql

Comprehensive template for creating Flyway migration scripts. Includes:

- **Header Template**: Version, description, author, dependencies
- **Base Domain Pattern**: Standard columns for all entities (id, uid, timestamps)
- **Ownable Domain Pattern**: Additional columns for tenant-scoped entities (owner_id, ref_id)
- **Foreign Key Template**: Pattern for child tables with relationships
- **Complete Example**: Full unit table implementation
- **Best Practices**: Inline comments with migration guidelines

### Usage

1. Copy `migration-template.sql` to migration directory:
   ```bash
   cp specs/005-ampairs-backend-ampairs/contracts/migration-template.sql \
      ampairs-backend/{module}/src/main/resources/db/migration/mysql/V4_{n}__create_{module}_tables.sql
   ```

2. Replace placeholders:
   - `{version}`  4.1, 4.2, etc.
   - `{module_name}`  "Customer Module", "Product Module"
   - `{author}`  Your name
   - `{date}`  Current date (YYYY-MM-DD)
   - `{table_name}`  Actual table name (snake_case)
   - `{column_definitions}`  Column definitions from JPA entity

3. Remove unused sections:
   - Remove `OwnableBaseDomain` fields if entity extends `BaseDomain` only
   - Remove foreign key sections if no relationships

4. Add entity-specific columns from JPA entity `@Column` annotations

5. Add indexes from JPA entity `@Table(indexes = [...])`

6. Add foreign keys for `@ManyToOne`, `@OneToOne` relationships

## Column Type Reference

See [../data-model.md](../data-model.md) for complete mapping table.

**Common Mappings**:
- `String`  `VARCHAR(length)`
- `String` (TEXT)  `TEXT`
- `Int`  `INT`
- `Double`  `DOUBLE`
- `Boolean`  `BOOLEAN`
- `Instant`  `TIMESTAMP`
- `JSON`  `JSON`

## Foreign Key Cascade Rules

| JPA Pattern | CASCADE Rule | Use Case |
|-------------|--------------|----------|
| `@OneToMany` | `ON DELETE CASCADE` | Delete children with parent |
| `@ManyToOne` | `ON DELETE RESTRICT` | Cannot delete if children exist |
| `@OneToOne` (nullable) | `ON DELETE SET NULL` | Null reference on delete |
| `@OneToOne` (not null) | `ON DELETE CASCADE` | Delete cascade |

## Index Guidelines

**Always Create**:
- Primary key: Automatic via `PRIMARY KEY (id)`
- Unique constraint on uid: `UNIQUE INDEX idx_{table}_uid (uid)`

**Recommended**:
- Foreign key columns: `INDEX idx_{table}_{column} (foreign_key_column)`
- Frequently queried columns: `INDEX idx_{table}_{column} (name, email, status)`
- Unique business keys: `UNIQUE INDEX uk_{table}_{column} (sku, gst_number)`

**Multi-Tenant**:
- Include `owner_id` in composite indexes for tenant filtering
- Example: `INDEX idx_{table}_owner_name (owner_id, name)`

## Complete Examples

See [../research.md#appendix-c](../research.md#appendix-c-migration-file-templates) for additional template examples:

- Template 1: Simple entity table
- Template 2: Child table with foreign keys
- Template 3: Migration file header

## Validation Checklist

Before committing migration:

- [ ] Header includes version, description, author, date
- [ ] All BaseDomain fields included (id, uid, timestamps)
- [ ] OwnableBaseDomain fields included if tenant-scoped
- [ ] Column types match JPA entity field types
- [ ] VARCHAR lengths match `@Column(length=...)`
- [ ] NOT NULL matches `nullable` attribute
- [ ] Indexes created from `@Table(indexes = [...])`
- [ ] Foreign keys reference uid (VARCHAR(200)), not id
- [ ] CASCADE rules match JPA relationship patterns
- [ ] Uses TIMESTAMP, not DATETIME
- [ ] Uses ENGINE=InnoDB
- [ ] Uses CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci

## Related Documentation

- **Quickstart Guide**: [../quickstart.md](../quickstart.md) - Step-by-step migration creation
- **Research Document**: [../research.md](../research.md) - Comprehensive patterns and decisions
- **Data Model**: [../data-model.md](../data-model.md) - Entity-to-table mappings
- **Implementation Plan**: [../plan.md](../plan.md) - Technical context and constitution compliance
